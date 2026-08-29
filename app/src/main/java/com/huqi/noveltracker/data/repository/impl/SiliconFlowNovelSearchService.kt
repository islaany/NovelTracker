package com.huqi.noveltracker.data.repository.impl

import android.util.Log
import com.huqi.noveltracker.data.model.TagCatalog
import com.huqi.noveltracker.data.repository.NovelSearchResult
import com.huqi.noveltracker.data.repository.NovelSearchService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Real novel lookup backed by an OpenAI-compatible LLM API (DeepSeek by default,
 * /v1/chat/completions).
 *
 * Flow: the caller passes the OCR text from a reading-app screenshot; the model
 * extracts the clean book title + author and, from its own knowledge, produces a
 * concise synopsis, protagonist(s), highlights and category tags. This is what
 * makes "pick screenshot -> know what the book is about" actually work end-to-end.
 *
 * Falls back to the same provider's chat model if the primary call errors, and
 * ultimately returns at least the raw title so the record can always be saved.
 */
class SiliconFlowNovelSearchService(
    private val apiKey: String,
    private val baseUrl: String = "https://api.deepseek.com/v1/",
    private val primaryModel: String = "deepseek-chat",
    private val fallbackModel: String = "deepseek-chat",
    /** Only this model family supports DeepSeek's server-side web_search tool. */
    private val searchModel: String = "deepseek-v4-flash"
) : NovelSearchService {

    // Web search browses and reads pages, so it needs a much longer read timeout.
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(150, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json".toMediaType()

    /** ".../v1/" -> ".../responses" — the Responses API lives outside /v1. */
    private val responsesUrl: String = baseUrl.substringBefore("/v1/") + "/responses"
    private val balanceUrl: String = baseUrl.substringBefore("/v1/") + "/user/balance"

    /**
     * Words the user has created or kept outside the built-in catalog. Fed back into
     * the prompt so the model gradually learns the user's own vocabulary (e.g.
     * BL-specific words it would otherwise never produce) while the built-in list
     * still guarantees every tag stays filterable.
     */
    // Named 'personalVocabulary' (not 'userVocabulary') on purpose: a public var
    // named userVocabulary would generate a setUserVocabulary() JVM setter that
    // clashes with the interface override below.
    private var personalVocabulary: List<String> = emptyList()

    override fun setUserVocabulary(words: List<String>) { personalVocabulary = words }

    private fun buildSystemPrompt(): String {
        val extra = personalVocabulary.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        val vocab = if (extra.isEmpty()) TagCatalog.promptList
        else TagCatalog.promptList + "、" + extra.joinToString("、")
        return """
        你是一个小说资料助手。用户会给你一段从手机阅读软件截图里 OCR 出来的文字（可能包含：页面顶部/书籍信息栏的书名、章节标题如"第一章 xxx"、作者、简介片段、正文段落等噪声）。
        请从中准确识别这本小说的：
        - 书名（重要：优先取页面最上方或书籍信息栏里的书名；不要把"第一章 xxx"这种章节标题当成书名。若分辨不清，取正文前最像书名的那一行）
        - 作者
        并基于你的知识生成：
        - synopsis：120字以内的剧情简介（世界观/主线/看点）
        - protagonist：主角名（多个用逗号分隔）
        - highlights：3-5条高光/名场面，每条以"· "开头，换行分隔
        - tags：3-5个分类标签，必须从下面的题材库中**原样挑选最贴切的词**（不要改写、不要造近义词、不要遗漏"文/流"等后缀，例如"都市异能"请拆成"都市"和"异能"两个库内词）：
        $vocab
        只输出严格 JSON，不要任何解释或 Markdown 代码块，格式：
        {"title":"","author":"","synopsis":"","protagonist":"","highlights":"","tags":["",""]}
        """.trimIndent()
    }

    override suspend fun search(title: String): NovelSearchResult? = withContext(Dispatchers.IO) {
        val query = title.trim()
        if (query.isBlank()) return@withContext null

        // A raw OCR dump is long; a short string is already a clean title (manual entry).
        val bookTitle = if (query.length > 40) {
            extractTitle(query)
                ?: query.lines().firstOrNull { it.isNotBlank() }?.take(40)
                ?: query
        } else query

        // 1) Preferred path: the model really searches the web, so the author and
        //    synopsis come from citations instead of guesses.
        searchWeb(bookTitle)?.let { return@withContext it }

        // 2) Search unavailable — fall back to knowledge-only generation, but flag
        //    the result as unverified so the UI can warn instead of presenting
        //    guesses as fact.
        val userContent = "以下是 OCR 文本：\n$query"
        for (model in listOf(primaryModel, fallbackModel)) {
            val content = callModel(model, userContent)
            if (!content.isNullOrBlank()) {
                return@withContext parse(content, bookTitle).copy(
                    source = "AI · 未联网核实",
                    found = false
                )
            }
        }
        // Everything failed: keep the title so the record can still be saved manually.
        NovelSearchResult(title = bookTitle, source = "OCR", found = false)
    }

    /**
     * Grounded lookup via the Responses API + the server-side `web_search` tool.
     * DeepSeek runs the search itself, so no third-party search key is needed.
     */
    private fun searchWeb(bookTitle: String): NovelSearchResult? {
        return try {
            val body = JSONObject().apply {
                put("model", searchModel)
                put("tools", JSONArray().put(JSONObject().put("type", "web_search")))
                put("text", JSONObject().put("format", JSONObject().put("type", "json_object")))
                put("input", webPrompt(bookTitle))
            }.toString()

            val request = Request.Builder()
                .url(responsesUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(body.toRequestBody(mediaType))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w("NovelAI", "web search http ${response.code}: ${response.message}")
                return null
            }
            val bodyStr = response.body?.string() ?: return null
            val root = JSONObject(bodyStr)
            // The Responses API always includes an "error" key; it is null on success.
            if (!root.isNull("error")) {
                Log.w("NovelAI", "web search error: ${root.opt("error")}")
                return null
            }

            val output = root.optJSONArray("output") ?: return null
            var text: String? = null
            for (i in 0 until output.length()) {
                val item = output.optJSONObject(i) ?: continue
                if (item.optString("type") != "message") continue
                val content = item.optJSONArray("content") ?: continue
                for (c in 0 until content.length()) {
                    val part = content.optJSONObject(c) ?: continue
                    if (part.optString("type") == "output_text") text = part.optString("text")
                }
            }
            if (text.isNullOrBlank()) return null
            parse(text, bookTitle).copy(source = "AI · 联网搜索")
        } catch (e: Exception) {
            Log.w("NovelAI", "web search failed: ${e.message}")
            null
        }
    }

    private fun webPrompt(bookTitle: String): String = """
        你是一个小说资料助手，并且**必须先调用联网搜索工具查证**，再回答。
        任务：查证并整理小说《$bookTitle》的真实资料。

        严格要求：
        1. 必须先联网搜索，不得只凭记忆作答。
        2. 严禁编造：任何字段如果在搜索结果中找不到可靠依据，就返回空字符串，并把 found 设为 false。
        3. 特别禁止：不要把小说正文或截图里出现的人物名字当成作者。
        4. author 填作者最常用的笔名（如"墨香铜臭"），本名可放在笔名后的括号里；查不到就留空字符串。
        5. synopsis：120字以内的剧情简介；protagonist：主角名；highlights：3-5条高光/名场面，每条以"· "开头，换行分隔。
        6. tags：从下面的题材库中挑选 3-5 个最贴切的，**原样使用库中的词，不要改写、不要自造近义词**：
        ${TagCatalog.promptList}
        7. sources：列出你实际参考的 2-4 个来源，每条格式为 "标题|URL"。

        只输出严格 JSON，不要任何解释或 Markdown 代码块：
        {"title":"","author":"","synopsis":"","protagonist":"","highlights":"","tags":[""],"found":true,"sources":["标题|URL"]}
    """.trimIndent()

    /** Cheap call that turns a raw OCR dump into a clean book title. */
    private fun extractTitle(ocr: String): String? {
        return try {
            val body = JSONObject().apply {
                put("model", primaryModel)
                put("messages", JSONArray().apply {
                    put(JSONObject().put("role", "user").put("content",
                        "下面是从阅读软件截图 OCR 出来的文字。请只输出这本小说的书名" +
                            "（不要解释、不要标点、不要章节名）：\n$ocr"))
                })
                put("max_tokens", 50)
                put("temperature", 0.1)
            }.toString()

            val request = Request.Builder()
                .url(baseUrl + "chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(body.toRequestBody(mediaType))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val bodyStr = response.body?.string() ?: return null
            JSONObject(bodyStr)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
                .takeIf { it.isNotBlank() && it.length <= 60 }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getBalanceCny(): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(balanceUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .get()
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null
            val bodyStr = response.body?.string() ?: return@withContext null
            val infos = JSONObject(bodyStr).optJSONArray("balance_infos") ?: return@withContext null
            val first = infos.optJSONObject(0) ?: return@withContext null
            first.optString("total_balance").takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    private fun callModel(model: String, userContent: String): String? {
        return try {
            val body = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", buildSystemPrompt()))
                    put(JSONObject().put("role", "user").put("content", userContent))
                })
                put("response_format", JSONObject().put("type", "json_object"))
                put("temperature", 0.3)
                put("max_tokens", 1200)
            }.toString()

            val request = Request.Builder()
                .url(baseUrl + "chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(body.toRequestBody(mediaType))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w("NovelAI", "model $model http ${response.code}: ${response.message}")
                return null
            }
            val bodyStr = response.body?.string() ?: return null
            JSONObject(bodyStr)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        } catch (e: Exception) {
            Log.w("NovelAI", "model $model error: ${e.message}")
            null
        }
    }

    private fun parse(content: String, fallbackTitle: String): NovelSearchResult {
        val clean = content
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()
        return try {
            val j = JSONObject(clean)
            val tags = mutableListOf<String>()
            j.optJSONArray("tags")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optString(i).takeIf { it.isNotBlank() }?.let { raw ->
                        val norm = TagCatalog.normalize(raw)
                        if (norm !in tags) tags.add(norm)
                    }
                }
            }
            val sources = mutableListOf<String>()
            j.optJSONArray("sources")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optString(i).takeIf { it.isNotBlank() }?.let { sources.add(it) }
                }
            }

            NovelSearchResult(
                title = j.optString("title").ifBlank { fallbackTitle },
                author = j.optString("author").ifBlank { null },
                coverUrl = null,
                synopsis = j.optString("synopsis").ifBlank { null },
                protagonist = j.optString("protagonist").ifBlank { null },
                highlights = j.optString("highlights").ifBlank { null },
                tags = tags,
                source = "AI · DeepSeek",
                found = j.optBoolean("found", true),
                sources = sources
            )
        } catch (e: Exception) {
            // Model returned non-JSON; keep it as synopsis so nothing is lost, but
            // mark it unverified.
            NovelSearchResult(
                title = fallbackTitle,
                synopsis = content.takeIf { it.isNotBlank() },
                source = "AI-raw",
                found = false
            )
        }
    }
}
