package com.huqi.noveltracker.ui.screen.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.huqi.noveltracker.appContainer
import com.huqi.noveltracker.data.model.Novel
import com.huqi.noveltracker.data.model.Tag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.comparisons.compareBy
import kotlin.comparisons.compareByDescending

enum class SortMode { RECENT, TITLE, MAIN_FIRST }

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = application.appContainer.novelRepository

    val tags: StateFlow<List<Tag>> =
        repo.observeTags().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _novels: StateFlow<List<Novel>> =
        repo.observeNovels().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _sortMode = MutableStateFlow(SortMode.RECENT)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    val filteredNovels: StateFlow<List<Novel>> =
        combine(_novels, _selectedTag, _query, _sortMode) { list, tag, q, sort ->
            // 1) Tag-chip filter (browse by tapping a genre pill).
            var res = if (tag == null) list else list.filter { tag in it.mainTags || tag in it.subTags }

            // 2) Free-text search — matches main/sub tag names or the title.
            val query = q.trim()
            if (query.isNotEmpty()) {
                val lower = query.lowercase()
                res = res.filter { novel ->
                    novel.mainTags.any { it.lowercase().contains(lower) } ||
                    novel.subTags.any { it.lowercase().contains(lower) } ||
                    novel.title.lowercase().contains(lower)
                }
            }

            // 3) Sort. When a tag/query is active, main-tag matches always rank first
            //    (e.g. searching "玄幻" pushes novels whose MAIN tag is 玄幻 to the top).
            val rankKey = if (query.isNotBlank()) query else (tag ?: "")
            res = if (rankKey.isNotBlank()) {
                res.sortedWith(mainFirst(rankKey).then(tiebreak(sort)))
            } else {
                when (sort) {
                    SortMode.TITLE -> res.sortedBy { it.title.lowercase() }
                    SortMode.MAIN_FIRST -> res.sortedWith(
                        compareByDescending<Novel> { it.mainTags.size }.thenByDescending { it.addedAt }
                    )
                    else -> res.sortedByDescending { it.addedAt }
                }
            }
            res
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTag(tag: String?) { _selectedTag.value = tag }
    fun setQuery(text: String) { _query.value = text }
    fun setSortMode(mode: SortMode) { _sortMode.value = mode }

    fun delete(novel: Novel) = viewModelScope.launch {
        repo.delete(novel)
    }

    /** Rank: main-tag match = 0, sub-tag match = 1, otherwise 2. */
    private fun mainFirst(key: String): Comparator<Novel> {
        val k = key.lowercase()
        return compareBy { novel ->
            when {
                novel.mainTags.any { it.lowercase().contains(k) } -> 0
                novel.subTags.any { it.lowercase().contains(k) } -> 1
                else -> 2
            }
        }
    }

    private fun tiebreak(sort: SortMode): Comparator<Novel> = when (sort) {
        SortMode.TITLE -> compareBy { it.title.lowercase() }
        else -> compareByDescending { it.addedAt }
    }
}
