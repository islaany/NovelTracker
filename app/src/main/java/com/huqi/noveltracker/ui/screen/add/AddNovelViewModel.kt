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
import com.huqi.noveltracker.data.model.TagCatalog
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AddStep { PICK, IMPORTING, REVIEW }
enum class ImportPhase { OCR, SEARCH, DONE }

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

    private val _importPhase = MutableStateFlow(ImportPhase.OCR)
    val importPhase: StateFlow<ImportPhase> = _importPhase.asStateFlow()

    private val _ocrText = MutableStateFlow("")
    val ocrText: StateFlow<String> = _ocrText.asStateFlow()

    private val _draft = MutableStateFlow(AddDraft())
    val draft: StateFlow<AddDraft> = _draft.asStateFlow()

    private val _mainTags = MutableStateFlow<Set<String>>(emptySet())
    val mainTags: StateFlow<Set<String>> = _mainTags.asStateFlow()

    private val _subTags = MutableStateFlow<Set<String>>(emptySet())
    val subTags: StateFlow<Set<String>> = _subTags.asStateFlow()

    private val _wantReRead = MutableStateFlow(false)
    val wantReRead: StateFlow<Boolean> = _wantReRead.asStateFlow()

    private val _wantRecommend = MutableStateFlow(false)
    val wantRecommend: StateFlow<Boolean> = _wantRecommend.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun clearError() { _error.value = null }

    // ---- flow ----

    fun onImagePicked(uri: Uri) {
        _error.value = null
        _imageUri.value = uri
        _step.value = AddStep.IMPORTING
        runImport(uri)
    }

    /** Re-run the whole OCR + AI pipeline on the same screenshot (used from the review screen). */
    fun reImport() {
        _imageUri.value?.let { onImagePicked(it) }
    }

    private fun runImport(uri: Uri) {
        viewModelScope.launch {
            try {
                // 1) OCR
                _importPhase.value = ImportPhase.OCR
                val bitmap = uri.toBitmap(getApplication())
                if (bitmap == null) {
                    _error.value = "无法读取这张图片，请换一张，或用手动输入书名"
                    _step.value = AddStep.PICK
                    return@launch
                }
                val text = runCatching { ocrEngine.recognize(bitmap) }.getOrDefault("")
                _ocrText.value = text

                // 2) AI lookup — send the WHOLE OCR text (capped) so the model can find the
                //    book title / author wherever they appear, instead of only the first line
                //    (which is often a chapter title like "第一章 xxx", not the book name).
                _importPhase.value = ImportPhase.SEARCH
                val firstLine = text.lines().firstOrNull { it.isNotBlank() } ?: ""
                val query = text.lines().take(60).joinToString("\n").trim().take(2000)
                if (query.isBlank()) {
                    _error.value = "没从截图上识别到文字，请手动输入书名"
                    _step.value = AddStep.PICK
                    return@launch
                }
                val result = runCatching { searchService.search(query) }.getOrNull()

                _draft.value = AddDraft(
                    title = result?.title ?: firstLine,
                    author = result?.author ?: "",
                    coverUrl = result?.coverUrl ?: "",
                    synopsis = result?.synopsis ?: "",
                    protagonist = result?.protagonist ?: "",
                    highlights = result?.highlights ?: "",
                    source = result?.source ?: ""
                )
                // Normalize AI tags onto the curated genre vocabulary so the
                // selected set always matches what's filterable in the catalog.
                // AI-suggested tags land in sub-tags; the user promotes up to 2
                // to main tags in the review screen.
                _subTags.value = (result?.tags ?: emptyList<String>())
                    .map { TagCatalog.normalize(it) }
                    .toSet()
                _mainTags.value = emptySet()

                // 3) finished — show "导入完成" briefly, then advance to the editable review screen
                _importPhase.value = ImportPhase.DONE
                delay(900)
                _step.value = AddStep.REVIEW
            } catch (e: Exception) {
                _error.value = "导入失败：${e.message ?: e.javaClass.simpleName}"
                _step.value = AddStep.PICK
            }
        }
    }

    /**
     * Manual path: skip the screenshot/OCR step entirely and let the AI generate the
     * record from a title the user typed. Guarantees a working flow even if the image
     * picker or on-device OCR misbehaves on a particular device.
     */
    fun startManualSearch(rawTitle: String) {
        val title = rawTitle.trim()
        if (title.isBlank()) { _error.value = "请输入书名"; return }
        _error.value = null
        _ocrText.value = title
        _step.value = AddStep.IMPORTING
        _importPhase.value = ImportPhase.SEARCH
        viewModelScope.launch {
            try {
                val result = runCatching { searchService.search(title) }.getOrNull()
                _draft.value = AddDraft(
                    title = result?.title ?: title,
                    author = result?.author ?: "",
                    coverUrl = result?.coverUrl ?: "",
                    synopsis = result?.synopsis ?: "",
                    protagonist = result?.protagonist ?: "",
                    highlights = result?.highlights ?: "",
                    source = result?.source ?: ""
                )
                // Normalize AI tags onto the curated genre vocabulary so the
                // selected set always matches what's filterable in the catalog.
                // AI-suggested tags land in sub-tags; the user promotes up to 2
                // to main tags in the review screen.
                _subTags.value = (result?.tags ?: emptyList<String>())
                    .map { TagCatalog.normalize(it) }
                    .toSet()
                _mainTags.value = emptySet()
                _importPhase.value = ImportPhase.DONE
                delay(900)
                _step.value = AddStep.REVIEW
            } catch (e: Exception) {
                _error.value = "生成失败：${e.message ?: e.javaClass.simpleName}"
                _step.value = AddStep.PICK
            }
        }
    }

    fun updateDraft(block: (AddDraft) -> AddDraft) {
        _draft.value = block(_draft.value)
    }

    /** Toggle a main tag. Enforces the max-2 rule and moves it out of sub-tags. */
    fun toggleMain(name: String) {
        val main = _mainTags.value.toMutableSet()
        if (main.contains(name)) {
            main.remove(name)
        } else {
            if (main.size >= 2) return // 主标签最多 2 个
            main.add(name)
            val sub = _subTags.value.toMutableSet()
            sub.remove(name)
            _subTags.value = sub
        }
        _mainTags.value = main
    }

    /** Toggle a sub tag (unlimited). */
    fun toggleSub(name: String) {
        val sub = _subTags.value.toMutableSet()
        if (sub.contains(name)) sub.remove(name) else sub.add(name)
        _subTags.value = sub
    }

    /**
     * Add a free-form custom tag the user typed (covers genres missing from the
     * built-in catalog). Upserted into the catalog so it becomes filterable, and
     * immediately placed into the novel's sub-tags.
     */
    fun addCustomSubTag(raw: String) {
        val name = raw.trim()
        if (name.isBlank()) return
        // upsertTag is suspend — fire it on the viewmodel scope; the sub-tag set is
        // updated synchronously so the chip appears immediately.
        viewModelScope.launch { runCatching { repo.upsertTag(Tag(name = name, color = tagColor(name))) } }
        val sub = _subTags.value.toMutableSet()
        sub.add(name)
        _subTags.value = sub
    }

    fun toggleWantReRead() { _wantReRead.value = !_wantReRead.value }
    fun toggleWantRecommend() { _wantRecommend.value = !_wantRecommend.value }

    /** Persists the record. Returns the new id, or null on failure. */
    suspend fun save(): Long? {
        val d = _draft.value
        val finalTitle = d.title.ifBlank { _ocrText.value.lines().firstOrNull { it.isNotBlank() } ?: "" }
        val main = _mainTags.value.filter { it.isNotBlank() }
        val sub = _subTags.value.filter { it.isNotBlank() }
        // Make sure selected tags exist in the catalog so they become filterable on Home.
        (main + sub).forEach { name ->
            runCatching { repo.upsertTag(Tag(name = name, color = tagColor(name))) }
        }
        val novel = Novel(
            title = finalTitle,
            author = d.author.ifBlank { null },
            coverUrl = d.coverUrl.ifBlank { null },
            synopsis = d.synopsis.ifBlank { null },
            protagonist = d.protagonist.ifBlank { null },
            highlights = d.highlights.ifBlank { null },
            wantReRead = _wantReRead.value,
            wantRecommend = _wantRecommend.value,
            mainTags = main,
            subTags = sub,
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
        context.contentResolver.openInputStream(this)?.use { stream ->
            val raw = BitmapFactory.decodeStream(stream) ?: return@runCatching null
            // Downscale very large screenshots so OCR is fast and the whole image is
            // processed (ML Kit can otherwise be slow / truncate on huge bitmaps).
            val maxDim = 1600
            val maxSide = if (raw.width > raw.height) raw.width else raw.height
            val scale = if (maxSide > maxDim) maxDim.toFloat() / maxSide else 1f
            if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    raw,
                    (raw.width * scale).toInt(),
                    (raw.height * scale).toInt(),
                    true
                ).also { raw.recycle() }
            } else raw
        }
    }.getOrNull()
}
