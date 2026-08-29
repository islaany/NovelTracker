package com.huqi.noveltracker

import android.app.Application
import androidx.core.content.edit
import com.huqi.noveltracker.data.local.AppDatabase
import com.huqi.noveltracker.data.repository.NovelRepository
import com.huqi.noveltracker.data.repository.impl.RoomNovelRepository
import com.huqi.noveltracker.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application class. Holds the manual DI container so ViewModels can reach
 * repositories without Hilt (kept simple on purpose; swap to Hilt later if wanted).
 */
class NovelTrackerApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getDatabase(this)
        container = AppContainer(
            novelRepository = RoomNovelRepository(database.novelDao(), database.tagDao())
        )

        // Seed the curated genre vocabulary on every launch (idempotent; only adds,
        // never wipes user tags) so the tag catalog is always selectable.
        scope.launch { runCatching { container.novelRepository.ensureDefaultTags() } }

        // Seed a few demo novels on first launch so the UI is populated for review.
        val prefs = getSharedPreferences("noveltracker_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean("seeded", false)) {
            scope.launch {
                runCatching { container.novelRepository.seedSampleData() }
                prefs.edit { putBoolean("seeded", true) }
            }
        }
    }
}

val Application.appContainer: AppContainer
    get() = (this as NovelTrackerApplication).container
