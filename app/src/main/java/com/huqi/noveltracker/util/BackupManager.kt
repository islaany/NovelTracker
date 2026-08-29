package com.huqi.noveltracker.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.huqi.noveltracker.data.model.Novel
import com.huqi.noveltracker.data.model.Tag
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Local backup: the whole library (novels + tag catalog) serialized to a single
 * JSON file that can be shared to any app (drive, wechat, email…).
 *
 * Chosen over cloud sync on purpose — no server, no account, no cost, and the
 * data never leaves the user's control.
 */
object BackupManager {

    private const val AUTHORITY = "com.huqi.noveltracker.fileprovider"

    data class Backup(val novels: List<Novel>, val tags: List<Tag>)

    fun exportJson(context: Context, novels: List<Novel>, tags: List<Tag>): Uri? {
        if (novels.isEmpty() && tags.isEmpty()) return null

        val json = JSONObject().apply {
            put("app", "NovelTracker")
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())
            put("tags", JSONArray().apply {
                tags.forEach { t -> put(JSONObject().put("name", t.name).put("color", t.color)) }
            })
            put("novels", JSONArray().apply { novels.forEach { n -> put(n.toJson()) } })
        }

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date())
        val file = File(dir, "noveltracker-backup-$stamp.json")
        file.writeText(json.toString(2), Charsets.UTF_8)
        return FileProvider.getUriForFile(context, AUTHORITY, file)
    }

    fun share(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "导出书架备份"))
    }

    /**
     * Strip whitespace/punctuation/full-width variants so "全职高手 2" and "全职高手2"
     * compare equal. Shared by duplicate detection and backup restore.
     */
    fun normalizeTitle(t: String): String = t.trim().lowercase()
        .replace(Regex("[\\s\\p{Punct}　·—－_、，,。.!！?？:：;；\"'“”‘’()（）\\[\\]【】《》]"), "")

    fun readBackup(context: Context, uri: Uri): Backup? {
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.use {
                it.bufferedReader(Charsets.UTF_8).readText()
            }
        }.getOrNull()
        if (text.isNullOrBlank()) return null
        return runCatching { parse(text) }.getOrNull()
    }

    private fun parse(text: String): Backup {
        val root = JSONObject(text)

        val tags = mutableListOf<Tag>()
        root.optJSONArray("tags")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val name = o.optString("name")
                if (name.isNotBlank()) tags.add(Tag(name, o.optString("color", "#7C4DFF")))
            }
        }

        val novels = mutableListOf<Novel>()
        root.optJSONArray("novels")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val title = o.optString("title")
                if (title.isBlank()) continue
                novels.add(
                    Novel(
                        id = 0L,           // re-inserted; Room assigns new ids on merge
                        title = title,
                        author = o.optString("author").takeIf { it.isNotBlank() },
                        coverUrl = o.optString("coverUrl").takeIf { it.isNotBlank() },
                        synopsis = o.optString("synopsis").takeIf { it.isNotBlank() },
                        protagonist = o.optString("protagonist").takeIf { it.isNotBlank() },
                        highlights = o.optString("highlights").takeIf { it.isNotBlank() },
                        wantReRead = if (o.has("wantReRead")) o.optBoolean("wantReRead") else null,
                        wantRecommend = if (o.has("wantRecommend")) o.optBoolean("wantRecommend") else null,
                        mainTags = o.strList("mainTags"),
                        subTags = o.strList("subTags"),
                        source = o.optString("source").takeIf { it.isNotBlank() },
                        sources = o.strList("sources"),
                        addedAt = o.optLong("addedAt", System.currentTimeMillis())
                    )
                )
            }
        }
        return Backup(novels, tags)
    }

    private fun Novel.toJson(): JSONObject = JSONObject().apply {
        put("title", title)
        put("author", author ?: "")
        put("coverUrl", coverUrl ?: "")
        put("synopsis", synopsis ?: "")
        put("protagonist", protagonist ?: "")
        put("highlights", highlights ?: "")
        wantReRead?.let { put("wantReRead", it) }
        wantRecommend?.let { put("wantRecommend", it) }
        put("mainTags", JSONArray().apply { mainTags.forEach { put(it) } })
        put("subTags", JSONArray().apply { subTags.forEach { put(it) } })
        put("source", source ?: "")
        put("sources", JSONArray().apply { sources.forEach { put(it) } })
        put("addedAt", addedAt)
    }

    private fun JSONObject.strList(key: String): List<String> {
        val out = mutableListOf<String>()
        optJSONArray(key)?.let { arr ->
            for (i in 0 until arr.length()) {
                val v = arr.optString(i)
                if (v.isNotBlank()) out.add(v)
            }
        }
        return out
    }
}
