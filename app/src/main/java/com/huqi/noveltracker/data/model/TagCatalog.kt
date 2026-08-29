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
        // 玄幻 / 仙侠 / 武侠
        Tag("玄幻", "#7C4DFF"),
        Tag("奇幻", "#5C6BC0"),
        Tag("仙侠", "#26A69A"),
        Tag("修真", "#00897B"),
        Tag("武侠", "#00897B"),
        Tag("洪荒", "#5C4DFF"),
        Tag("高武", "#42A5F5"),
        Tag("国术", "#5C8A5C"),
        Tag("灵气复苏", "#9CCC65"),
        // 科幻 / 未来
        Tag("科幻", "#42A5F5"),
        Tag("星际", "#29B6F6"),
        Tag("机甲", "#66BB6A"),
        Tag("无限流", "#66BB6A"),
        Tag("赛博朋克", "#5C4DFF"),
        Tag("未来", "#4FC3F7"),
        // 末世 / 悬疑 / 灵异
        Tag("末世", "#EF5350"),
        Tag("丧尸", "#455A64"),
        Tag("求生", "#EF5350"),
        Tag("灾变", "#C99A3B"),
        Tag("悬疑", "#8E24AA"),
        Tag("推理", "#AB47BC"),
        Tag("刑侦", "#8E24AA"),
        Tag("探案", "#AB47BC"),
        Tag("谍战", "#6D4C41"),
        Tag("盗墓", "#5C6BC0"),
        Tag("诡异", "#5C4DFF"),
        Tag("惊悚", "#455A64"),
        Tag("灵异", "#5C6BC0"),
        Tag("恐怖", "#455A64"),
        // 历史 / 军事 / 权谋
        Tag("历史", "#C99A3B"),
        Tag("军事", "#6D4C41"),
        Tag("战争", "#6D4C41"),
        Tag("权谋", "#C99A3B"),
        Tag("宫斗", "#D81B60"),
        // 都市 / 校园 / 职场
        Tag("都市", "#26C6DA"),
        Tag("异能", "#42A5F5"),
        Tag("校园", "#4DD0E1"),
        Tag("职场", "#78909C"),
        Tag("娱乐圈", "#EC407A"),
        Tag("学霸", "#4DD0E1"),
        Tag("竞技", "#EF5350"),
        Tag("体育", "#EF5350"),
        Tag("商战", "#78909C"),
        // 言情 / 纯爱
        Tag("言情", "#EC407A"),
        Tag("纯爱", "#F06292"),
        Tag("百合", "#F48FB1"),
        Tag("古言", "#EC407A"),
        Tag("现言", "#F06292"),
        Tag("甜宠", "#F06292"),
        Tag("虐恋", "#D81B60"),
        Tag("豪门", "#D81B60"),
        Tag("总裁", "#EC407A"),
        Tag("闪婚", "#F48FB1"),
        // 穿越 / 重生 / 系统
        Tag("穿越", "#FF7043"),
        Tag("重生", "#FFA726"),
        Tag("快穿", "#FF7043"),
        Tag("穿书", "#FFA726"),
        Tag("女配", "#F48FB1"),
        Tag("系统", "#9CCC65"),
        Tag("无敌流", "#EF5350"),
        Tag("爽文", "#FF7043"),
        Tag("签到", "#9CCC65"),
        Tag("神豪", "#FFA726"),
        // 种田 / 美食 / 基建
        Tag("年代文", "#9CCC65"),
        Tag("基建", "#5C8A5C"),
        Tag("经营", "#5C8A5C"),
        Tag("种田", "#9CCC65"),
        Tag("美食", "#FFCA28"),
        // 群像 / 情绪
        Tag("脑洞", "#7C4DFF"),
        Tag("暗黑", "#455A64"),
        Tag("群像", "#7986CB"),
        Tag("热血", "#EF5350"),
        Tag("搞笑", "#FFB300"),
        Tag("治愈", "#4DB6AC"),
        // 西幻 / 二次元
        Tag("西幻", "#29B6F6"),
        Tag("龙与地下城", "#5C6BC0"),
        Tag("骑士", "#26A69A"),
        Tag("吸血鬼", "#8E24AA"),
        Tag("轻小说", "#7E57C2"),
        Tag("二次元", "#BA68C8"),
        Tag("萌系", "#F48FB1"),
        Tag("日常", "#4DD0E1"),
        Tag("同人", "#BA68C8"),
        // 游戏 / 直播
        Tag("网游", "#26A69A"),
        Tag("游戏", "#26A69A"),
        Tag("副本", "#42A5F5"),
        Tag("直播", "#FF7043"),
        Tag("综艺", "#EC407A"),
        // 频道
        Tag("男频", "#37474F"),
        Tag("女频", "#AD1457"),
        // BL / 耽美细分
        Tag("BL", "#5C6BC0"),
        Tag("耽美", "#7986CB"),
        Tag("ABO", "#8E24AA"),
        Tag("哨兵向导", "#5C4DFF"),
        Tag("年下", "#26C6DA"),
        Tag("年上", "#4DD0E1"),
        Tag("强强", "#EF5350"),
        Tag("主受", "#F06292"),
        Tag("主攻", "#EC407A"),
        Tag("破镜重圆", "#FF7043"),
        Tag("追妻火葬场", "#D81B60"),
        Tag("双洁", "#4DB6AC"),
        Tag("非双洁", "#78909C"),
        Tag("生子", "#F48FB1"),
        Tag("强制爱", "#D81B60"),
        Tag("替身", "#9575CD"),
        Tag("暗恋", "#9575CD"),
        Tag("青梅竹马", "#4DB6AC"),
        Tag("先婚后爱", "#EC407A"),
        Tag("契约恋爱", "#BA68C8"),
        Tag("掉马", "#FFB300"),
        Tag("救赎", "#26A69A"),
        Tag("养成", "#9CCC65"),
        Tag("甜虐", "#F06292"),
        Tag("双向奔赴", "#F48FB1"),
        Tag("误会", "#78909C"),
        Tag("团宠", "#FFCA28"),
        // 经典流派
        Tag("升级流", "#66BB6A"),
        Tag("蒸汽朋克", "#5C8A5C"),
        Tag("克苏鲁", "#5C4DFF")
    )

    private val nameSet: Set<String> = DEFAULT_GENRES.map { it.name }.toSet()

    /** Human-readable genre list for the AI prompt (e.g. "玄幻、奇幻、仙侠…"). */
    val promptList: String = DEFAULT_GENRES.joinToString("、") { it.name }

    /** True when [name] ships with the app; false for user-created / AI-invented words. */
    fun isBuiltIn(name: String): Boolean = name in nameSet

    /**
     * Common AI phrasings -> our catalog genre. Covers suffixes like "文/流/类"
     * and near-synonyms so the recommended tags land on a real, filterable chip.
     */
    private val SYNONYMS: Map<String, String> = mapOf(
        "玄幻魔法" to "玄幻", "东方玄幻" to "玄幻", "西方玄幻" to "西幻", "玄幻小说" to "玄幻",
        "高武世界" to "高武", "武道" to "国术", "武侠小说" to "武侠",
        "都市生活" to "都市", "都市异能" to "异能", "都市言情" to "都市", "都市情感" to "都市", "都市爽文" to "都市", "都市小说" to "都市",
        "女主" to "女频", "女主文" to "女频", "女频文" to "女频", "女生向" to "女频",
        "男主" to "男频", "男主文" to "男频", "男频文" to "男频", "男生向" to "男频",
        "甜文" to "甜宠", "甜宠文" to "甜宠", "宠文" to "甜宠", "甜" to "甜宠",
        "虐文" to "虐恋", "虐恋情深" to "虐恋", "be" to "虐恋", "悲剧" to "虐恋", "虐" to "虐恋",
        "快穿文" to "快穿", "穿书文" to "穿书", "重生文" to "重生", "重生流" to "重生",
        "穿越文" to "穿越", "穿越流" to "穿越",
        "年代" to "年代文", "穿越年代" to "年代文", "基建文" to "基建", "经营文" to "经营", "种田文" to "种田", "美食文" to "美食",
        "修仙" to "修真", "修仙文" to "修真", "修真小说" to "修真", "洪荒流" to "洪荒",
        "灵气" to "灵气复苏", "签到文" to "签到", "神豪文" to "神豪", "系统流" to "系统", "系统文" to "系统",
        "末日" to "末世", "末世危机" to "末世", "丧尸文" to "丧尸", "求生文" to "求生", "灾变文" to "灾变",
        "星际文" to "星际", "机甲文" to "机甲", "无限恐怖" to "无限流", "赛博" to "赛博朋克", "未来世界" to "未来",
        "刑侦悬疑" to "刑侦", "侦探" to "探案", "探案文" to "探案", "谍战特工" to "谍战",
        "盗墓笔记" to "盗墓", "灵异鬼怪" to "灵异", "鬼怪" to "灵异", "惊悚文" to "惊悚", "恐怖文" to "恐怖",
        "西式奇幻" to "西幻", "龙与地下城" to "龙与地下城", "吸血鬼文" to "吸血鬼",
        "轻小说" to "轻小说", "二次元" to "二次元", "萌系" to "萌系", "日常文" to "日常", "同人文" to "同人",
        "网游文" to "网游", "游戏" to "游戏", "副本文" to "副本", "直播文" to "直播", "综艺文" to "综艺", "电竞文" to "电竞",
        "群像剧" to "群像", "热血文" to "热血", "搞笑文" to "搞笑", "治愈系" to "治愈", "脑洞文" to "脑洞", "暗黑文" to "暗黑",
        "豪门文" to "豪门", "总裁文" to "总裁", "闪婚文" to "闪婚", "古言文" to "古言", "现言文" to "现言",
        "言情文" to "言情", "纯爱文" to "纯爱", "百合文" to "百合",
        "异能超能" to "异能", "超能" to "异能", "校园青春" to "校园", "青春" to "校园",
        "职场文" to "职场", "娱乐圈文" to "娱乐圈", "明星" to "娱乐圈", "学霸文" to "学霸",
        "竞技文" to "竞技", "体育文" to "体育", "商战文" to "商战",
        "无敌文" to "无敌流", "爽文" to "爽文", "升级流" to "升级流",
        "克苏鲁神话" to "克苏鲁", "蒸汽朋克" to "蒸汽朋克",
        "穿越重生" to "重生", "异界" to "玄幻", "异世界" to "奇幻"
    )

    /**
     * Map a free-form AI tag onto a catalog genre when possible.
     * Order: exact -> synonym -> strip trailing 文/流/类/小说/题材/向 then retry ->
     * substring match in either direction -> synonym substring -> original.
     * Returns the original string when nothing fits, so no information is lost.
     */
    fun normalize(raw: String): String {
        val t = raw.trim()
        if (t.isBlank()) return t
        if (t in nameSet) return t
        SYNONYMS[t]?.let { return it }
        val stripped = t.removeSuffix("文").removeSuffix("流").removeSuffix("类")
            .removeSuffix("小说").removeSuffix("题材").removeSuffix("向")
        if (stripped != t) {
            if (stripped in nameSet) return stripped
            SYNONYMS[stripped]?.let { return it }
        }
        nameSet.firstOrNull { t.contains(it) || it.contains(t) }?.let { return it }
        SYNONYMS.entries.firstOrNull { t.contains(it.key) }?.let { return it.value }
        return t
    }
}
