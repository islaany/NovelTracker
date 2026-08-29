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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.comparisons.compareBy
import kotlin.comparisons.compareByDescending

enum class SortMode { RECENT, TITLE, MAIN_FIRST }
enum class ListMode { ALL, WANT_REREAD, WANT_RECOMMEND }

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

    private val _listMode = MutableStateFlow(ListMode.ALL)
    val listMode: StateFlow<ListMode> = _listMode.asStateFlow()

    /**
     * Tags actually used by at least one novel — keeps the home filter clean
     * (no empty / redundant catalog entries piling up at the top).
     */
    val filterTags: StateFlow<List<Tag>> =
        combine(_novels, tags) { novels, allTags ->
            val used = novels.flatMap { it.mainTags + it.subTags }.toSet()
            allTags.filter { it.name in used }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** All novels the user marked 想推荐 (for one-tap export, independent of tag/query). */
    val recommendNovels: StateFlow<List<Novel>> =
        _novels.map { list -> list.filter { it.wantRecommend == true } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredNovels: StateFlow<List<Novel>> =
        combine(_novels, _selectedTag, _query, _sortMode, _listMode) { list, tag, q, sort, mode ->
            // 0) List mode (我的书架 / 想再看 / 想推荐) from the left drawer.
            var res = when (mode) {
                ListMode.WANT_REREAD -> list.filter { it.wantReRead == true }
                ListMode.WANT_RECOMMEND -> list.filter { it.wantRecommend == true }
                else -> list
            }

            // 1) Tag-chip filter (browse by tapping a genre pill).
            if (tag != null) res = res.filter { tag in it.mainTags || tag in it.subTags }

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
    fun setSortMode(m: SortMode) { _sortMode.value = m }
    fun setListMode(m: ListMode) { _listMode.value = m }

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
