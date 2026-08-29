package com.huqi.noveltracker.data.repository.impl

import com.huqi.noveltracker.data.local.NovelDao
import com.huqi.noveltracker.data.local.TagDao
import com.huqi.noveltracker.data.local.entity.toEntity
import com.huqi.noveltracker.data.local.entity.toModel
import com.huqi.noveltracker.data.model.Novel
import com.huqi.noveltracker.data.model.Tag
import com.huqi.noveltracker.data.model.TagCatalog
import com.huqi.noveltracker.data.repository.NovelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class RoomNovelRepository(
    private val novelDao: NovelDao,
    private val tagDao: TagDao
) : NovelRepository {

    override fun observeNovels(): Flow<List<Novel>> =
        novelDao.observeAll().map { list -> list.map { it.toModel() } }

    override fun observeNovel(id: Long): Flow<Novel?> =
        novelDao.observeById(id).map { it?.toModel() }

    override fun observeTags(): Flow<List<Tag>> =
        tagDao.observeAll().map { list -> list.map { it.toModel() } }

    override suspend fun upsert(novel: Novel): Long = novelDao.upsertAndGetId(novel.toEntity())
    override suspend fun delete(novel: Novel) = novelDao.delete(novel.toEntity())

    override suspend fun upsertTag(tag: Tag) = tagDao.upsert(tag.toEntity())
    override suspend fun deleteTag(tag: Tag) = tagDao.delete(tag.toEntity())

    override suspend fun ensureDefaultTags() {
        TagCatalog.DEFAULT_GENRES.forEach { tagDao.upsert(it.toEntity()) }
    }

    override suspend fun seedSampleData() {
        if (novelDao.observeAll().first().isNotEmpty()) return
        val tags = listOf(
            Tag("克苏鲁", "#7C4DFF"),
            Tag("蒸汽朋克", "#5C8A5C"),
            Tag("西幻", "#4FC3F7"),
            Tag("权谋", "#C99A3B"),
            Tag("穿越", "#E57373"),
            Tag("历史", "#BA68C8"),
            Tag("玄幻", "#FF8A65"),
            Tag("升级流", "#A1887F"),
            Tag("电竞", "#42A5F5"),
            Tag("热血", "#EF5350"),
            Tag("群像", "#26A69A")
        )
        tags.forEach { tagDao.upsert(it.toEntity()) }

        val novels = listOf(
            Novel(
                title = "诡秘之主",
                author = "爱潜水的乌贼",
                synopsis = "蒸汽与机械的时代，穿越者克莱恩背靠灰雾之上的愚者座椅，在非凡力量与隐秘组织交织的世界中寻找归途。",
                protagonist = "克莱恩·莫雷蒂（周明瑞）",
                highlights = "· 序列途径与魔药体系\n· 塔罗会布局\n· 对抗外神的最终决战",
                wantReRead = true,
                wantRecommend = true,
                tags = listOf("克苏鲁", "蒸汽朋克", "西幻"),
                source = "示例"
            ),
            Novel(
                title = "庆余年",
                author = "猫腻",
                synopsis = "现代青年穿越至庆国，以范闲之名周旋于庙堂与江湖，逐步揭开自己的身世与朝堂阴谋。",
                protagonist = "范闲",
                highlights = "· 江南诗会惊四座\n· 牛栏街刺杀\n· 京都风波及权争",
                wantReRead = false,
                wantRecommend = true,
                tags = listOf("权谋", "穿越", "历史"),
                source = "示例"
            ),
            Novel(
                title = "斗破苍穹",
                author = "天蚕土豆",
                synopsis = "天才少年萧炎一朝沦为废柴，得神秘老者药尘相助，踏上重整荣耀的修炼之路。",
                protagonist = "萧炎",
                highlights = "· 退婚流经典开局\n· 异火收集\n· 炎帝崛起",
                wantReRead = true,
                wantRecommend = false,
                tags = listOf("玄幻", "升级流"),
                source = "示例"
            ),
            Novel(
                title = "全职高手",
                author = "蝴蝶蓝",
                synopsis = "被俱乐部驱逐的荣耀顶尖选手叶修隐于网吧，以新人身份重返赛场，再攀巅峰。",
                protagonist = "叶修",
                highlights = "· 兴欣战队组建\n· 全明星赛\n· 第十赛季夺冠",
                wantReRead = false,
                wantRecommend = false,
                tags = listOf("电竞", "热血", "群像"),
                source = "示例"
            )
        )
        novels.forEach { novelDao.upsertAndGetId(it.toEntity()) }
    }
}
