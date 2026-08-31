package com.huqi.noveltracker.data.repository.impl

import android.util.Log
import com.huqi.noveltracker.data.model.TagCatalog
import com.huqi.noveltracker.data.repository.NovelSearchResult
import com.huqi.noveltracker.data.repository.NovelSearchService
import com.huqi.noveltracker.data.settings.SearchBackend
import com.huqi.noveltracker.data.settings.SearchConfig
import com.huqi.noveltracker.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** A single web-search hit returned by Tavily / Exa, used to synthesize the record. */
private data class SearchHit(val title: String, val url: String, val content: String)

/** What the LLM extracts from noisy OCR *before* we ever touch the search API. */
private data class OcrClean(val title: String, val author: String?, val query: String)

/**
 * Real novel lookup driven by a two-stage LLM + search pipeline. The key, base URL and
 * model are read at call time from the runtime Settings (DataStore), so the same APK works
 * with any OpenAI-compatible provider the user configures.
 *
 * Recommended pipeline (no DeepSeek required):
 *   ① SiliconFlow (free LLM) cleans the raw, noisy OCR → extracts the real book title /
 *     author and crafts the most accurate search query (decide WHAT to search).
 *   ② Tavily / Exa (dedicated search API) runs the real web search with that query.
 *   ③ SiliconFlow (free LLM) reads ALL crawled web snippets and outputs exactly the
 *     structured record we need (title/author/synopsis/tags/...), grounded in the snippets.
 *
 * The LLM is the "brain" on both ends; the search API is just the crawler. DeepSeek's
 * native web_search is an OPTIONAL alternative (only when the user explicitly enables it).
 * If no LLM key is configured we degrade gracefully to a heuristic query + the search
 * backend's own answer.
 */
class SiliconFlowNovelSearchService(
    private val settings: SettingsRepository
) : NovelSearchService {

    // Web search browses and reads pages, so it needs a much longer read timeout.
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(150, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json".toMediaType()

    /**
     * Words the user has created or kept outside the built-in catalog. Fed back into
     * the prompt so the model gradually learns the user's own vocabulary.
     */
    private var personalVocabulary: List<String> = emptyList()

    override fun setUserVocabulary(words: List<String>) { personalVocabulary = words }

    override suspend fun isConfigured(): Boolean =
        runCatching {
            val cfg = settings.getConfig()
            cfg.apiKey.isNotBlank() ||
                (cfg.searchBackend != SearchBackend.NONE && cfg.searchApiKey.isNotBlank())
        }.getOrDefault(false)

    /**
     * Two-stage "LLM brain + search crawler" pipeline:
     *   ① SiliconFlow cleans the raw, noisy OCR into an accurate query (what to search).
     *   ② Tavily / Exa performs the real web search with that query.
     *   ③ SiliconFlow reads all crawled snippets and outputs the structured record we need.
     * DeepSeek web_search is only used when the user explicitly enabled it (no dedicated
     * search backend). Falls back gracefully when an LLM key is absent.
     */
    override suspend fun search(ocrText: String): NovelSearchResult? = withContext(Dispatchers.IO) {
        val cfg = settings.getConfig()
        val raw = ocrText.trim()
        if (raw.isBlank()) return@withContext null

        val hasLLM = cfg.apiKey.isNotBlank() && cfg.baseUrl.isNotBlank()
        val hasSearch = cfg.searchBackend != SearchBackend.NONE && cfg.searchApiKey.isNotBlank()

        // ① Pre-process OCR (LLM) → accurate {title, author, query}. Heuristic fallback if no LLM key.
        val clean = if (hasLLM) {
            preprocessOcr(raw, cfg) ?: OcrClean(heuristicTitle(raw), null, heuristicTitle(raw) + " 小说 简介 资料")
        } else {
            OcrClean(heuristicTitle(raw), null, heuristicTitle(raw) + " 小说 简介 资料")
        }

        // ② Real web search with the cleaned query.
        if (hasSearch) {
            val (hits, answer) = when (cfg.searchBackend) {
                SearchBackend.TAVILY -> tavilySearch(clean.query, cfg.searchApiKey, cfg.searchMaxResults)
                SearchBackend.EXA -> exaSearch(clean.query, cfg.searchApiKey, cfg.searchMaxResults)
                else -> Pair(emptyList<SearchHit>(), null as String?)
            }
            if (hits.isNotEmpty() || !answer.isNullOrBlank()) {
                val backendName = if (cfg.searchBackend == SearchBackend.TAVILY) "Tavily" else "Exa"
                // ③ Synthesize the crawled results with the LLM into the record we need.
                if (hasLLM) {
                    synthesize(clean, hits, answer, backendName, cfg)?.let { return@withContext it }
                }
                // No-LLM fallback: use the search backend's own answer / top snippet directly.
                buildFromSearch(clean, hits, answer, backendName)?.let { return@withContext it }
            }
        }

        // ②-alt: no dedicated search backend, but user explicitly enabled DeepSeek web_search.
        if (!hasSearch && cfg.searchBackend == SearchBackend.NONE && cfg.webEnabled && cfg.isDeepSeek && cfg.apiKey.isNotBlank()) {
            searchWeb(raw, cfg)?.let { return@withContext it }
        }

        // ④ Knowledge-only generation (LLM present but web search yielded nothing). Flagged.
        if (hasLLM) {
            val content = callModel(cfg.chatModel, "以下是 OCR 文本：\n$raw", cfg)
            if (!content.isNullOrBlank()) {
                return@withContext parse(content, clean.title).copy(
                    source = "AI · 未联网核实",
                    found = false
                )
            }
        }

        // ⑤ Last resort: at least prefill title/author from OCR so the record isn't empty.
        val (t, a) = heuristicTitleAuthor(raw)
        NovelSearchResult(title = t, author = a, source = "OCR", found = false)
    }

    /**
     * Grounded lookup via the Responses API + the server-side `web_search` tool.
     * The full OCR text is sent so the model can extract title/author and search.
     */
    private fun searchWeb(fullText: String, cfg: SearchConfig): NovelSearchResult? {
        return try {
            val body = JSONObject().apply {
                put("model", cfg.searchModel.ifBlank { "deepseek-v4-flash" })
                put("tools", JSONArray().put(JSONObject().put("type", "web_search")))
                put("text", JSONObject().put("format", JSONObject().put("type", "json_object")))
                put("input", webPrompt(fullText))
            }.toString()

            val request = Request.Builder()
                .url(responsesUrl(cfg.baseUrl))
                .addHeader("Authorization", "Bearer ${cfg.apiKey}")
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
            val result = parse(text, heuristicTitle(fullText))
            // Only accept when the model actually produced something; otherwise fall
            // back so the user never gets a silently-empty "not found" record.
            val hasData = result.author != null || result.synopsis != null ||
                result.tags.isNotEmpty() || result.title.isNotBlank()
            if (!hasData) return null
            result.copy(source = "AI · 联网搜索")
        } catch (e: Exception) {
            Log.w("NovelAI", "web search failed: ${e.message}")
            null
        }
    }

    /**
     * ① Pre-process: feed the raw, noisy OCR to the LLM and ask it to extract the real book
     * title + author and craft the most accurate search query. We never send the raw OCR to
     * the search API as-is, because it is full of chapter bodies / UI noise that would ruin
     * the query. Returns null on failure so the caller can fall back to a heuristic query.
     */
    private fun preprocessOcr(raw: String, cfg: SearchConfig): OcrClean? {
        val prompt = """
你是一个小说信息提取助手。下面是从手机阅读软件截图里 OCR 识别出来的文字，包含大量噪声：章节标题（如"第一章 xxx"）、正文段落、UI 文字、广告等。
请从中准确识别这本小说的**书名**和**作者**，并生成一个最适合联网搜索的查询词。

要求：
1. title：小说书名。优先取页面最上方或书籍信息栏里的书名；绝对不要把"第一章 xxx"这种章节标题当成书名。若实在无法判断，返回 OCR 里最像书名的那一行。
2. author：作者（常见笔名）。若文字里看不到作者就返回空字符串。
3. query：用于联网搜索的最佳查询词，建议格式为"书名 作者 小说 简介 资料"（作者未知则只用"书名 小说 资料"）。目标是让搜索引擎最容易命中这本小说的权威资料页。

只输出严格 JSON，不要任何解释或 Markdown 代码块：
{"title":"","author":"","query":""}

OCR 原文：
$raw
""".trimIndent()
        val json = callModel(
            cfg.chatModel, prompt, cfg,
            system = "你是一个严谨的信息提取助手。只输出用户要求的 JSON，不要任何解释或 Markdown 代码块。"
        ) ?: return null
        return try {
            val j = JSONObject(
                json.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            )
            val title = j.optString("title").takeIf { it.isNotBlank() } ?: heuristicTitle(raw)
            val author = j.optString("author").takeIf { it.isNotBlank() }
            val query = j.optString("query").takeIf { it.isNotBlank() } ?: "$title 小说 简介 资料"
            OcrClean(title, author, query)
        } catch (e: Exception) {
            Log.w("NovelAI", "preprocess parse fail: ${e.message}")
            null
        }
    }

    /**
     * ③ Synthesize: give the LLM ALL the crawled web snippets (and the engine's own answer)
     * and have it output exactly the structured record we need, strictly grounded in the
     * snippets. Returns null if the model produced nothing usable.
     */
    private fun synthesize(
        clean: OcrClean,
        hits: List<SearchHit>,
        answer: String?,
        backendName: String,
        cfg: SearchConfig
    ): NovelSearchResult? {
        val snippets = buildString {
            if (!answer.isNullOrBlank()) append("搜索引擎摘要：\n$answer\n\n")
            if (hits.isNotEmpty()) append(
                hits.joinToString("\n\n") { "【${it.title}】(${it.url})\n${it.content}" }
            )
        }.trim()
        if (snippets.isBlank()) return null
        val titleHint = clean.title + (if (!clean.author.isNullOrBlank()) "（作者：${clean.author}）" else "")
        val prompt = """
你是一个小说资料助手。下面是从联网搜索引擎（$backendName）**真实检索**到的网页片段，请**严格依据这些片段**来整理这本小说的信息。

书名线索：$titleHint

联网检索到的网页片段：
$snippets

要求：
1. 从片段中确认书名和作者（不要把正文里的人物名当作者）。
2. 简介、主角、高光、标签都应基于网页片段内容；片段中找不到的字段留空字符串。
3. tags 从题材库原样挑选 3-5 个：${TagCatalog.promptList}
4. 严禁编造：找不到可靠依据的字段就返回空字符串。

只输出严格 JSON，不要任何解释或 Markdown 代码块：
{"title":"","author":"","synopsis":"","protagonist":"","highlights":"","tags":[""],"found":true,"sources":["标题|URL"]}
""".trimIndent()
        val json = callModel(
            cfg.chatModel, prompt, cfg,
            system = "你是一个严谨的小说资料整理助手。严格依据给定的网页片段输出 JSON，绝不凭记忆编造。"
        ) ?: return null
        val result = parse(json, clean.title)
        val hasData = result.author != null || result.synopsis != null ||
            result.tags.isNotEmpty() || result.title.isNotBlank()
        if (!hasData) return null
        return result.copy(
            source = "AI · 联网搜索($backendName)",
            found = true,
            sources = result.sources.ifEmpty { hits.map { "${it.title}|${it.url}" } }
        )
    }

    /** No-LLM fallback: turn the search backend's own answer / top snippet into a minimal record. */
    private fun buildFromSearch(
        clean: OcrClean,
        hits: List<SearchHit>,
        answer: String?,
        backendName: String
    ): NovelSearchResult? {
        val top = hits.firstOrNull()
        val synopsis = (answer ?: top?.content).takeIf { !it.isNullOrBlank() }?.toString()?.take(400)
        val author = clean.author ?: parseAuthor(top?.content ?: answer ?: "")
        if (synopsis.isNullOrBlank() && author.isNullOrBlank()) return null
        return NovelSearchResult(
            title = clean.title,
            author = author,
            synopsis = synopsis,
            tags = emptyList(),
            source = "联网搜索($backendName)",
            found = true,
            sources = hits.map { "${it.title}|${it.url}" }
        )
    }

    /** Pull an author name out of web-snippet text (handles "作者：xxx" / "作者简介：xxx"). */
    private fun parseAuthor(text: String): String? {
        if (text.isBlank()) return null
        val m = Regex("(?im)(?:作者|著)\\s*[:：]?\\s*([\\u4e00-\\u9fa5A-Za-z0-9_·]{1,20})").find(text)
        return m?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
    }

    /** Tavily search API → real web snippets + a synthesized answer (free tier ~1000/month). */
    private fun tavilySearch(query: String, key: String, maxResults: Int): Pair<List<SearchHit>, String?> {
        return try {
            val body = JSONObject().apply {
                put("api_key", key)
                put("query", query)
                put("max_results", maxResults)
                put("search_depth", "advanced")
                put("include_answer", true)
                put("include_raw_content", false)
            }.toString()
            val request = Request.Builder()
                .url("https://api.tavily.com/search")
                .addHeader("Content-Type", "application/json")
                .post(body.toRequestBody(mediaType))
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w("NovelAI", "tavily http ${response.code}: ${response.message}")
                return Pair(emptyList<SearchHit>(), null as String?)
            }
            val root = JSONObject(response.body?.string() ?: return Pair(emptyList<SearchHit>(), null as String?))
            val arr = root.optJSONArray("results") ?: org.json.JSONArray()
            val hits = mutableListOf<SearchHit>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                hits.add(SearchHit(o.optString("title", ""), o.optString("url", ""), o.optString("content", "")))
            }
            val answer = root.optString("answer", "").takeIf { it.isNotBlank() }
            hits to answer
        } catch (e: Exception) {
            Log.w("NovelAI", "tavily error: ${e.message}")
            Pair(emptyList<SearchHit>(), null as String?)
        }
    }

    /** Exa search API → real web snippets (free tier available). */
    private fun exaSearch(query: String, key: String, maxResults: Int): Pair<List<SearchHit>, String?> {
        return try {
            val body = JSONObject().apply {
                put("query", query)
                put("numResults", maxResults)
                put("contents", JSONObject().put("text", true).put("summary", true))
            }.toString()
            val request = Request.Builder()
                .url("https://api.exa.ai/search")
                .addHeader("Authorization", "Bearer $key")
                .addHeader("Content-Type", "application/json")
                .post(body.toRequestBody(mediaType))
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w("NovelAI", "exa http ${response.code}: ${response.message}")
                return Pair(emptyList<SearchHit>(), null as String?)
            }
            val root = JSONObject(response.body?.string() ?: return Pair(emptyList<SearchHit>(), null as String?))
            val arr = root.optJSONArray("results") ?: org.json.JSONArray()
            val hits = mutableListOf<SearchHit>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val text = o.optJSONObject("text")?.optString("text") ?: o.optString("text", "")
                val summary = o.optString("summary", "")
                hits.add(SearchHit(o.optString("title", ""), o.optString("url", ""), summary.ifBlank { text }))
            }
            val answer = hits.firstOrNull()?.content?.takeIf { it.isNotBlank() }
            hits to answer
        } catch (e: Exception) {
            Log.w("NovelAI", "exa error: ${e.message}")
            Pair(emptyList<SearchHit>(), null as String?)
        }
    }

    private fun webPrompt(fullText: String): String = """
        你是一个小说资料助手，并且**必须先调用联网搜索工具查证**，再回答。
        任务：先识别下面 OCR 文字描述的是哪本小说（书名 + 作者），再**联网搜索**核实其真实资料。

        OCR 文字（含可能的书名、作者、章节、简介、正文等噪声）：
        $fullText

        严格要求：
        1. 先从上面的文字里准确识别这本小说的书名和作者（若已是干净书名则直接用；出现多个候选时取最像书名的那个）。
        2. 然后**必须联网搜索**这本书（用识别出的书名，必要时结合作者），不得只凭记忆作答。
        3. 严禁编造：任何字段若在搜索结果中找不到可靠依据，就返回空字符串，并把 found 设为 false。
        4. 特别禁止：不要把小说正文或截图里出现的人物名字当成作者。
        5. author 填作者最常用的笔名；查不到就留空字符串。
        6. synopsis：120字以内的剧情简介；protagonist：主角名；highlights：3-5条高光/名场面，每条以"· "开头，换行分隔。
        7. tags：从下面的题材库中挑选 3-5 个最贴切的，**原样使用库中的词，不要改写、不要自造近义词**：
        ${TagCatalog.promptList}
        8. sources：列出你实际参考的 2-4 个来源，每条格式为 "标题|URL"。

        只输出严格 JSON，不要任何解释或 Markdown 代码块：
        {"title":"","author":"","synopsis":"","protagonist":"","highlights":"","tags":[""],"found":true,"sources":["标题|URL"]}
    """.trimIndent()

    private fun callModel(model: String, userContent: String, cfg: SearchConfig, system: String? = null): String? {
        return try {
            val body = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", system ?: buildSystemPrompt()))
                    put(JSONObject().put("role", "user").put("content", userContent))
                })
                put("response_format", JSONObject().put("type", "json_object"))
                put("temperature", 0.3)
                put("max_tokens", 1200)
            }.toString()

            val request = Request.Builder()
                .url(chatUrl(cfg.baseUrl))
                .addHeader("Authorization", "Bearer ${cfg.apiKey}")
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

    override suspend fun getBalanceCny(): String? = withContext(Dispatchers.IO) {
        val cfg = settings.getConfig()
        if (!cfg.isDeepSeek) return@withContext null
        try {
            val request = Request.Builder()
                .url(balanceUrl(cfg.baseUrl))
                .addHeader("Authorization", "Bearer ${cfg.apiKey}")
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

    /** Best-effort title + author pulled straight from OCR when the AI returns nothing. */
    private fun heuristicTitleAuthor(text: String): Pair<String, String?> {
        val title = Regex("《([^》]{1,40})》").find(text)?.groupValues?.get(1)
            ?: Regex("(?im)书名[\\s:：]+(\\S.{0,30})").find(text)?.groupValues?.get(1)?.trim()
            ?: text.lines().firstOrNull { it.isNotBlank() }?.take(40)
            ?: text.take(40)
        val author = Regex("(?im)作[者著][\\s:：]+(\\S.{0,20})").find(text)?.groupValues?.get(1)?.trim()
            ?: Regex("(?im)作者[：: ]*(\\S{1,20})").find(text)?.groupValues?.get(1)?.trim()
        return title to author
    }

    private fun heuristicTitle(text: String): String = heuristicTitleAuthor(text).first

    private fun responsesUrl(baseUrl: String): String =
        baseUrl.substringBefore("/v1/") + "/responses"

    private fun chatUrl(baseUrl: String): String =
        baseUrl.trimEnd('/') + "/chat/completions"

    private fun balanceUrl(baseUrl: String): String =
        baseUrl.substringBefore("/v1/") + "/user/balance"
}
