package com.huqi.noveltracker.ui.screen.add

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.huqi.noveltracker.appContainer
import com.huqi.noveltracker.data.model.Novel
import com.huqi.noveltracker.data.model.Tag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AddStep { PICK, OCR, NAME, REVIEW }

data class AddDraft(
    val title: String = "",
    val author: String = "",
    val coverUrl: String = "",
    val synopsis: String = "",
    val protagonist: String = "",
    val highlights: String = "",
    val source: String = ""
)

class AddNovelViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = application.appContainer.novelRepository
    private val ocrEngine = application.appContainer.ocrEngine
    private val searchService = application.appContainer.novelSearchService

    val tags: StateFlow<List<Tag>> =
        repo.observeTags().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _step = MutableStateFlow<AddStep>(AddStep.PICK)
    val step: StateFlow<AddStep> = _step.asStateFlow()

    private val _imageUri = MutableStateFlow<Uri?>(null)
    val imageUri: StateFlow<Uri?> = _imageUri.asStateFlow()

    private val _ocrText = MutableStateFlow("")
    val ocrText: StateFlow<String> = _ocrText.asStateFlow()

    private val _isOcrRunning = MutableStateFlow(false)
    val isOcrRunning: StateFlow<Boolean> = _isOcrRunning.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _draft = MutableStateFlow(AddDraft())
    val draft: StateFlow<AddDraft> = _draft.asStateFlow()

    private val _selectedTags = MutableStateFlow<Set<String>>(emptySet())
    val selectedTags: StateFlow<Set<String>> = _selectedTags.asStateFlow()

    private val _wantReRead = MutableStateFlow(false)
    val wantReRead: StateFlow<Boolean> = _wantReRead.asStateFlow()

    private val _wantRecommend = MutableStateFlow(false)
    val wantRecommend: StateFlow<Boolean> = _wantRecommend.asStateFlow()

    // ---- flow ----

    fun onImagePicked(uri: Uri) {
        _imageUri.value = uri
        _step.value = AddStep.OCR
        runOcr(uri)
    }

    private fun runOcr(uri: Uri) {
        viewModelScope.launch {
            _isOcrRunning.value = true
            val bitmap = uri.toBitmap(getApplication())
            val text = bitmap?.let { runCatching { ocrEngine.recognize(it) }.getOrDefault("") } ?: ""
            _ocrText.value = text
            _title.value = text.lines().firstOrNull { it.isNotBlank() } ?: ""
            _isOcrRunning.value = false
            _step.value = AddStep.NAME
        }
    }

    fun onTitleChange(value: String) { _title.value = value }
    fun onOcrTextChange(value: String) { _ocrText.value = value }

    fun onSearch() {
        // Prefer the (possibly corrected) OCR text so the AI can extract the real title;
        // fall back to the manual title field when OCR produced nothing.
        val q = _ocrText.value.trim().ifBlank { _title.value.trim() }
        if (q.isBlank()) return
        viewModelScope.launch {
            _isSearching.value = true
            val result = runCatching { searchService.search(q) }.getOrNull()
            _isSearching.value = false
            val r = result
            _draft.value = AddDraft(
                title = r?.title ?: q,
                author = r?.author ?: "",
                coverUrl = r?.coverUrl ?: "",
                synopsis = r?.synopsis ?: "",
                protagonist = r?.protagonist ?: "",
                highlights = r?.highlights ?: "",
                source = r?.source ?: ""
            )
            _selectedTags.value = (r?.tags ?: emptyList<String>()).toSet()
            _step.value = AddStep.REVIEW
        }
    }

    fun updateDraft(block: (AddDraft) -> AddDraft) {
        _draft.value = block(_draft.value)
    }

    fun toggleTag(name: String) {
        val set = _selectedTags.value.toMutableSet()
        if (set.contains(name)) set.remove(name) else set.add(name)
        _selectedTags.value = set
    }

    fun toggleWantReRead() { _wantReRead.value = !_wantReRead.value }
    fun toggleWantRecommend() { _wantRecommend.value = !_wantRecommend.value }

    /** Persists the record. Returns the new id, or null on failure. */
    suspend fun save(): Long? {
        val d = _draft.value
        val tags = _selectedTags.value.filter { it.isNotBlank() }
        // Make sure selected tags exist in the catalog so they become filterable on Home.
        tags.forEach { name ->
            runCatching { repo.upsertTag(Tag(name = name, color = tagColor(name))) }
        }
        val novel = Novel(
            title = d.title.ifBlank { _title.value },
            author = d.author.ifBlank { null },
            coverUrl = d.coverUrl.ifBlank { null },
            synopsis = d.synopsis.ifBlank { null },
            protagonist = d.protagonist.ifBlank { null },
            highlights = d.highlights.ifBlank { null },
            wantReRead = _wantReRead.value,
            wantRecommend = _wantRecommend.value,
            tags = tags,
            source = d.source.ifBlank { null }
        )
        return runCatching { repo.upsert(novel) }.getOrNull()?.takeIf { id -> id > 0L }
    }

    /** Deterministic pleasant color for a tag name so the catalog chips look varied. */
    private fun tagColor(name: String): String {
        val palette = listOf(
            "#7C4DFF", "#5C8A5C", "#4FC3F7", "#C99A3B", "#E57373",
            "#BA68C8", "#FF8A65", "#A1887F", "#42A5F5", "#26A69A", "#EF5350"
        )
        val idx = name.sumOf { it.code } % palette.size
        return palette[idx]
    }

    private fun Uri.toBitmap(context: Context): Bitmap? = runCatching {
        context.contentResolver.openInputStream(this)?.use {
            BitmapFactory.decodeStream(it)
        }
    }.getOrNull()
}
