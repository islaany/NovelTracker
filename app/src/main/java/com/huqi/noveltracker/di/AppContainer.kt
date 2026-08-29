package com.huqi.noveltracker.di

import com.huqi.noveltracker.BuildConfig
import com.huqi.noveltracker.data.repository.NovelRepository
import com.huqi.noveltracker.data.repository.NovelSearchService
import com.huqi.noveltracker.data.repository.OcrEngine
import com.huqi.noveltracker.data.repository.impl.MlKitOcrEngine
import com.huqi.noveltracker.data.repository.impl.SiliconFlowNovelSearchService

/**
 * Manual dependency container (no Hilt, on purpose — simpler to read/adjust).
 * All swappable implementations live here. The online lookup now uses the
 * SiliconFlow LLM (OCR text -> structured novel record). Swap for a different
 * provider by constructing another NovelSearchService here.
 */
data class AppContainer(
    val novelRepository: NovelRepository,
    val ocrEngine: OcrEngine = MlKitOcrEngine(),
    val novelSearchService: NovelSearchService = SiliconFlowNovelSearchService(
        apiKey = BuildConfig.SILICONFLOW_API_KEY,
        baseUrl = BuildConfig.SILICONFLOW_BASE_URL
    )
)
