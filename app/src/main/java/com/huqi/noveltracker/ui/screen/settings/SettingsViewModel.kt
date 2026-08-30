package com.huqi.noveltracker.ui.screen.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.huqi.noveltracker.appContainer
import com.huqi.noveltracker.data.settings.SearchConfig
import com.huqi.noveltracker.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repo: SettingsRepository = application.appContainer.settingsRepository

    private val _draft = MutableStateFlow(SearchConfig())
    val draft: StateFlow<SearchConfig> = _draft.asStateFlow()

    init { viewModelScope.launch { runCatching { _draft.value = repo.getConfig() } } }

    fun update(block: (SearchConfig) -> SearchConfig) { _draft.value = block(_draft.value) }

    fun applyPreset(preset: SearchConfig) { _draft.value = preset }

    fun save() { viewModelScope.launch { runCatching { repo.save(_draft.value) } } }
}
