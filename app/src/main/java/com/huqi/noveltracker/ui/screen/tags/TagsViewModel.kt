package com.huqi.noveltracker.ui.screen.tags

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.huqi.noveltracker.appContainer
import com.huqi.noveltracker.data.model.Tag
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TagsViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = application.appContainer.novelRepository

    val tags: StateFlow<List<Tag>> =
        repo.observeTags().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addTag(name: String, color: String) = viewModelScope.launch {
        if (name.isNotBlank()) repo.upsertTag(Tag(name.trim(), color))
    }

    fun deleteTag(tag: Tag) = viewModelScope.launch {
        repo.deleteTag(tag)
    }
}
