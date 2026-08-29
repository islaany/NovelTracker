package com.huqi.noveltracker.data.model

/**
 * Domain model for a tracked novel.
 * UI and repository speak in this type; Room entities are mapped to/from it.
 */
data class Novel(
    val id: Long = 0,
    val title: String,
    val author: String? = null,
    val coverUrl: String? = null,
    val synopsis: String? = null,
    val protagonist: String? = null,
    val highlights: String? = null,
    val wantReRead: Boolean? = null,
    val wantRecommend: Boolean? = null,
    /** Primary classification tags — at most 2 per novel. */
    val mainTags: List<String> = emptyList(),
    /** Finer-grained tags — unlimited. */
    val subTags: List<String> = emptyList(),
    val source: String? = null,
    val addedAt: Long = System.currentTimeMillis()
) {
    /** Backwards-compatible union of main + sub tags (used by cards/filters). */
    val tags: List<String> get() = mainTags + subTags
}

/**
 * Tag catalog entry (name + display color, hex string like "#7C4DFF").
 */
data class Tag(
    val name: String,
    val color: String
)
