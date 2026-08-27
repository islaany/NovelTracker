package com.huqi.noveltracker.di

import com.huqi.noveltracker.data.repository.NovelRepository
import com.huqi.noveltracker.data.repository.NovelSearchService
import com.huqi.noveltracker.data.repository.OcrEngine
import com.huqi.noveltracker.data.repository.impl.MockNovelSearchService
import com.huqi.noveltracker.data.repository.impl.MlKitOcrEngine

/**
 * Manual dependency container (no Hilt, on purpose — simpler to read/adjust).
 * All swappable implementations live here; e.g. replace MockNovelSearchService
 * with a real API-backed client when wiring the online lookup.
 */
data class AppContainer(
    val novelRepository: NovelRepository,
    val ocrEngine: OcrEngine = MlKitOcrEngine(),
    val novelSearchService: NovelSearchService = MockNovelSearchService()
)
