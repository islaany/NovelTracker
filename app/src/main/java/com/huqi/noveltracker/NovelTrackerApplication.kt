package com.huqi.noveltracker

import android.app.Application
import com.huqi.noveltracker.data.local.AppDatabase
import com.huqi.noveltracker.data.repository.NovelRepository
import com.huqi.noveltracker.data.repository.impl.RoomNovelRepository
import com.huqi.noveltracker.di.AppContainer

/**
 * Application class. Holds the manual DI container so ViewModels can reach
 * repositories without Hilt (kept simple on purpose; swap to Hilt later if wanted).
 */
class NovelTrackerApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getDatabase(this)
        container = AppContainer(
            novelRepository = RoomNovelRepository(database.novelDao(), database.tagDao())
        )
    }
}

val Application.appContainer: AppContainer
    get() = (this as NovelTrackerApplication).container
