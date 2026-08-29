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
    private val fallbackModel: String = "deepseek-chat"
) : NovelSearchService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json".toMediaType()

    private val systemPrompt = """
        你是一个小说资料助手。用户会给你一段从手机阅读软件截图里 OCR 出来的文字（可能包含：页面顶部/书籍信息栏的书名、章节标题如"第一章 xxx"、作者、简介片段、正文段落等噪声）。
        请从中准确识别这本小说的：
        - 书名（重要：优先取页面最上方或书籍信息栏里的书名；不要把"第一章 xxx"这种章节标题当成书名。若分辨不清，取正文前最像书名的那一行）
        - 作者
        并基于你的知识生成：
        - synopsis：120字以内的剧情简介（世界观/主线/看点）
        - protagonist：主角名（多个用逗号分隔）
        - highlights：3-5条高光/名场面，每条以"· "开头，换行分隔
        - tags：3-5个分类标签，必须从下面的题材库中选择最贴切的（不要自创新词，也不要用"灵气复苏"这类不在库里的词）：
        ${TagCatalog.promptList}
        只输出严格 JSON，不要任何解释或 Markdown 代码块，格式：
        {"title":"","author":"","synopsis":"","protagonist":"","highlights":"","tags":["",""]}
    """.trimIndent()

    override suspend fun search(title: String): NovelSearchResult? = withContext(Dispatchers.IO) {
        val query = title.trim()
        if (query.isBlank()) return@withContext null

        val userContent = "以下是 OCR 文本：\n$query"
        for (model in listOf(primaryModel, fallbackModel)) {
            val content = callModel(model, userContent)
            if (!content.isNullOrBlank()) {
                return@withContext parse(content, query)
            }
        }
        // All models failed (e.g. offline / no balance): still return the title so the
        // record can be saved and the user can fill the rest manually.
        NovelSearchResult(title = query, source = "OCR")
    }

    private fun callModel(model: String, userContent: String): String? {
        return try {
            val body = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", systemPrompt))
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
            NovelSearchResult(
                title = j.optString("title").ifBlank { fallbackTitle },
                author = j.optString("author").ifBlank { null },
                coverUrl = null,
                synopsis = j.optString("synopsis").ifBlank { null },
                protagonist = j.optString("protagonist").ifBlank { null },
                highlights = j.optString("highlights").ifBlank { null },
                tags = tags,
                source = "AI · DeepSeek"
            )
        } catch (e: Exception) {
            // Model returned non-JSON; keep it as synopsis so nothing is lost.
            NovelSearchResult(title = fallbackTitle, synopsis = content.takeIf { it.isNotBlank() }, source = "AI-raw")
        }
    }
}
