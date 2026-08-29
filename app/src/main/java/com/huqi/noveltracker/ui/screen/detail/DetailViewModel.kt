package com.huqi.noveltracker.ui.screen.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.huqi.noveltracker.appContainer
import com.huqi.noveltracker.data.model.Novel
import com.huqi.noveltracker.data.model.Tag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DetailViewModel(
    application: Application,
    private val novelId: Long
) : AndroidViewModel(application) {

    private val repo = application.appContainer.novelRepository

    val novel: StateFlow<Novel?> = repo.observeNovel(novelId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val tags: StateFlow<List<Tag>> =
        repo.observeTags().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleReRead() = viewModelScope.launch {
        novel.value?.let {
            repo.upsert(it.copy(wantReRead = !(it.wantReRead ?: false)))
        }
    }

    fun toggleRecommend() = viewModelScope.launch {
        novel.value?.let {
            repo.upsert(it.copy(wantRecommend = !(it.wantRecommend ?: false)))
        }
    }

    fun delete(onDeleted: () -> Unit) = viewModelScope.launch {
        novel.value?.let {
            repo.delete(it)
            onDeleted()
        }
    }

    class Factory(
        private val application: Application,
        private val novelId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DetailViewModel(application, novelId) as T
    }
}
