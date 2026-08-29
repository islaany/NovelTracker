package com.huqi.noveltracker.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.huqi.noveltracker.data.model.Novel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds a clean, printable HTML "推荐书单" from the novels the user marked
 * 想推荐, then shares it via a system chooser (WeChat / 邮件 / 浏览器打印成 PDF…).
 */
object RecommendExporter {

    private const val AUTHORITY = "com.huqi.noveltracker.fileprovider"

    /** Write the doc to cache and return a content:// Uri for sharing. Null if empty. */
    fun exportHtml(context: Context, novels: List<Novel>): Uri? {
        if (novels.isEmpty()) return null
        val html = buildHtml(novels)
        val dir = File(context.cacheDir, "exports").also { it.mkdirs() }
        val file = File(dir, "推荐书单_${stamp()}.html")
        file.writeText(html, Charsets.UTF_8)
        return FileProvider.getUriForFile(context, AUTHORITY, file)
    }

    fun share(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/html"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享推荐书单"))
    }

    private fun stamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmm", Locale.CHINA).format(Date())

    private fun esc(s: String?): String = (s ?: "").let {
        it.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    }

    private fun buildHtml(novels: List<Novel>): String {
        val date = SimpleDateFormat("yyyy 年 M 月 d 日", Locale.CHINA).format(Date())
        val cards = novels.joinToString("\n") { n ->
            val tagChips = (n.mainTags + n.subTags).joinToString(" ") { t ->
                """<span class="tag">${esc(t)}</span>"""
            }
            val highlights = n.highlights
                ?.lines()
                ?.filter { it.isNotBlank() }
                ?.joinToString("") { "<li>${esc(it.removePrefix("·").trim())}</li>" }
                .orEmpty()
            """
            <section class="book">
              <h2>${esc(n.title)}</h2>
              <div class="meta">${esc(n.author ?: "作者未知")}</div>
              <div class="tags">$tagChips</div>
              ${if (!n.synopsis.isNullOrBlank())
                """<div class="label">简介</div><p>${esc(n.synopsis)}</p>""" else ""}
              ${if (highlights.isNotBlank())
                """<div class="label">高光 / 看点</div><ul>$highlights</ul>""" else ""}
            </section>
            """.trimIndent()
        }

        return """
        <!DOCTYPE html>
        <html lang="zh-CN">
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>我的推荐书单</title>
        <style>
          :root { --accent:#7C4DFF; --paper:#FBF7F0; --ink:#2B2B33; }
          * { box-sizing:border-box; }
          body { margin:0; background:var(--paper); color:var(--ink);
                 font-family:-apple-system,"PingFang SC","Microsoft YaHei",sans-serif;
                 line-height:1.7; }
          .wrap { max-width:760px; margin:0 auto; padding:40px 28px 64px; }
          header { border-bottom:3px solid var(--accent); padding-bottom:16px; margin-bottom:28px; }
          h1 { margin:0; font-size:30px; letter-spacing:1px; }
          .sub { color:#8a8a93; margin-top:6px; font-size:14px; }
          .book { background:#fff; border:1px solid #efe9df; border-radius:14px;
                  padding:22px 24px; margin-bottom:20px; page-break-inside:avoid; }
          .book h2 { margin:0 0 4px; font-size:21px; color:var(--accent); }
          .meta { color:#6b6b73; font-size:14px; margin-bottom:10px; }
          .tags { margin:6px 0 14px; }
          .tag { display:inline-block; background:#EDE7FF; color:#5b3fd6;
                 font-size:12px; padding:3px 10px; border-radius:50px; margin:0 6px 6px 0; }
          .label { font-size:12px; letter-spacing:2px; color:#a39; text-transform:uppercase;
                   color:#9b8fb5; margin:14px 0 4px; font-weight:600; }
          p { margin:4px 0; }
          ul { margin:4px 0; padding-left:20px; }
          li { margin:2px 0; }
          @media print { body { background:#fff; } .book { box-shadow:none; } }
        </style>
        </head>
        <body>
          <div class="wrap">
            <header>
              <h1>我的推荐书单</h1>
              <div class="sub">共 ${novels.size} 本 · 生成于 $date</div>
            </header>
            $cards
          </div>
        </body>
        </html>
        """.trimIndent()
    }
}
