package com.huqi.noveltracker.data.repository

/**
 * A single novel's fetched metadata, used to pre-fill the record before saving.
 */
data class NovelSearchResult(
    val title: String,
    val author: String? = null,
    val coverUrl: String? = null,
    val synopsis: String? = null,
    val protagonist: String? = null,
    val highlights: String? = null,
    val tags: List<String> = emptyList(),
    val source: String? = null
)

/**
 * Online lookup for a novel by title.
 *
 * Framework ships a MockNovelSearchService so the UI flow is fully usable.
 * Later, replace with a real client backed by one of:
 *  - zhuishushenqi (追书神器) API: /book/{id} -> 简介/标签/评分
 *  - owllook_api: /v1/novels/{name}/{source} -> novel_abstract (主角信息)
 *  - Amibk/novel-api: /search/{keyword} -> 小说信息/目录
 */
interface NovelSearchService {
    suspend fun search(title: String): NovelSearchResult?
}
