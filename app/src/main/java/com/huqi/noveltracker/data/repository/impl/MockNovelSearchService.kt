package com.huqi.noveltracker.data.repository.impl

import com.huqi.noveltracker.data.repository.NovelSearchResult
import com.huqi.noveltracker.data.repository.NovelSearchService
import kotlinx.coroutines.delay

/**
 * Placeholder search service. Returns canned metadata so the whole add-flow
 * is clickable end-to-end before the real API is wired in.
 *
 * To go live: implement NovelSearchService against zhuishushenqi / owllook /
 * novel-api (see NovelSearchService.kt) and swap the instance in AppContainer.
 */
class MockNovelSearchService : NovelSearchService {
    override suspend fun search(title: String): NovelSearchResult? {
        delay(700) // simulate network latency
        if (title.isBlank()) return null
        return NovelSearchResult(
            title = title,
            author = "（示例）作者名",
            coverUrl = null,
            synopsis = "这里将显示从小说简介 API 拉取的剧情梗概、世界观与主角的设定。" +
                "接入真实 API 后会自动填充，方便你快速判断要不要再看/推荐给别人。",
            protagonist = "（示例）主角名",
            highlights = "· 高光情节一\n· 高光情节二\n· 高光情节三",
            tags = listOf("待分类"),
            source = "Mock"
        )
    }
}
