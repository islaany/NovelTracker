package com.huqi.noveltracker.data.model

/**
 * The fixed, curated genre vocabulary the user can pick from.
 *
 * Why this exists: previously the tag catalog was empty on a fresh install (the
 * sample seed only runs when the DB is empty), so after importing a book the only
 * "tags" were whatever free-form words the AI returned — nothing the user could
 * actually choose from, and the words rarely matched real genres.
 *
 * [DEFAULT_GENRES] is seeded into the `tags` table on every launch (idempotent
 * upsert, never deletes user-created tags). The AI is instructed to pick from
 * this list, and [normalize] maps any stray AI word onto the closest genre.
 */
object TagCatalog {

    val DEFAULT_GENRES: List<Tag> = listOf(
        Tag("玄幻", "#7C4DFF"),
        Tag("奇幻", "#5C6BC0"),
        Tag("仙侠", "#26A69A"),
        Tag("武侠", "#00897B"),
        Tag("科幻", "#42A5F5"),
        Tag("末世", "#EF5350"),
        Tag("悬疑", "#8E24AA"),
        Tag("推理", "#AB47BC"),
        Tag("灵异", "#5C6BC0"),
        Tag("恐怖", "#455A64"),
        Tag("历史", "#C99A3B"),
        Tag("军事", "#6D4C41"),
        Tag("都市", "#26C6DA"),
        Tag("言情", "#EC407A"),
        Tag("纯爱", "#F06292"),
        Tag("百合", "#F48FB1"),
        Tag("穿越", "#FF7043"),
        Tag("重生", "#FFA726"),
        Tag("系统", "#9CCC65"),
        Tag("无限流", "#66BB6A"),
        Tag("西幻", "#29B6F6"),
        Tag("游戏", "#26A69A"),
        Tag("电竞", "#42A5F5"),
        Tag("轻小说", "#7E57C2"),
        Tag("同人", "#BA68C8"),
        Tag("种田", "#9CCC65"),
        Tag("美食", "#FFCA28"),
        Tag("校园", "#4DD0E1"),
        Tag("职场", "#78909C"),
        Tag("权谋", "#C99A3B"),
        Tag("宫斗", "#D81B60"),
        Tag("治愈", "#4DB6AC"),
        Tag("群像", "#7986CB"),
        Tag("热血", "#EF5350"),
        Tag("搞笑", "#FFB300"),
        Tag("升级流", "#66BB6A"),
        Tag("克苏鲁", "#5C4DFF"),
        Tag("蒸汽朋克", "#5C8A5C")
    )

    private val nameSet: Set<String> = DEFAULT_GENRES.map { it.name }.toSet()

    /** Human-readable genre list for the AI prompt (e.g. "玄幻、奇幻、仙侠…"). */
    val promptList: String = DEFAULT_GENRES.joinToString("、") { it.name }

    /**
     * Map a free-form AI tag onto a catalog genre when possible.
     * Exact match first; otherwise a substring match in either direction
     * (e.g. "东方玄幻" -> "玄幻", "都市异能" -> "都市"). Returns the original
     * string when nothing fits, so no information is lost.
     */
    fun normalize(raw: String): String {
        val t = raw.trim()
        if (t.isBlank()) return t
        if (t in nameSet) return t
        nameSet.firstOrNull { t.contains(it) || it.contains(t) }?.let { return it }
        return t
    }
}
