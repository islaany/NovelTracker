package com.huqi.noveltracker.di

import com.huqi.noveltracker.data.repository.NovelRepository
import com.huqi.noveltracker.data.repository.NovelSearchService
import com.huqi.noveltracker.data.repository.OcrEngine
import com.huqi.noveltracker.data.repository.impl.MlKitOcrEngine
import com.huqi.noveltracker.data.repository.impl.SiliconFlowNovelSearchService
import com.huqi.noveltracker.data.settings.SettingsRepository

/**
 * Manual dependency container (no Hilt, on purpose — simpler to read/adjust).
 * All swappable implementations live here. The online lookup now reads its API
 * key / provider from the runtime Settings (DataStore) so the key is never
 * compiled into the build. Swap providers by constructing another NovelSearchService here.
 */
data class AppContainer(
    val novelRepository: NovelRepository,
    val ocrEngine: OcrEngine = MlKitOcrEngine(),
    val settingsRepository: SettingsRepository,
    val novelSearchService: NovelSearchService = SiliconFlowNovelSearchService(settingsRepository)
)
