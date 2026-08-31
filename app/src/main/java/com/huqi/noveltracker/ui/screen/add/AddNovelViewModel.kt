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
import com.huqi.noveltracker.util.BackupManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AddStep { PICK, IMPORTING, REVIEW }
enum class ImportPhase { OCR, SEARCH, DONE }

/** What "save" should do when the same book already exists in the library. */
enum class SaveAction { CREATE, UPDATE_EXISTING }

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

    /** Filters the tag picker so the user can find a tag instead of scrolling ~120 chips. */
    private val _tagQuery = MutableStateFlow("")
    val tagQuery: StateFlow<String> = _tagQuery.asStateFlow()

    /** Set when the imported title already exists in the library (duplicate warning). */
    private val _duplicate = MutableStateFlow<Novel?>(null)
    val duplicate: StateFlow<Novel?> = _duplicate.asStateFlow()

    /** Sources the AI actually consulted, "标题|URL" — shown so the user can verify. */
    private val _sources = MutableStateFlow<List<String>>(emptyList())
    val sources: StateFlow<List<String>> = _sources.asStateFlow()

    /** True when the data came back from a real web search, false = unverified. */
    private val _verified = MutableStateFlow(false)
    val verified: StateFlow<Boolean> = _verified.asStateFlow()

    /** Remaining DeepSeek balance (CNY) so the user can see what an import costs. */
    private val _balance = MutableStateFlow<String?>(null)
    val balance: StateFlow<String?> = _balance.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init { refreshBalance() }

    fun refreshBalance() {
        viewModelScope.launch {
            _balance.value = runCatching { searchService.getBalanceCny() }.getOrNull()
        }
    }

    fun clearError() { _error.value = null }
    fun setTagQuery(q: String) { _tagQuery.value = q }

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
                feedUserVocabulary()
                if (!runCatching { searchService.isConfigured() }.getOrDefault(false)) {
                    _error.value = "请先在「设置 → 联网搜索」填入 Tavily 或 Exa 的 Key（负责真实联网检索），并建议在「AI 模型」填入硅基流动 Key（负责先从 OCR 提取准确书名/查询词、再合成最终资料，无需 DeepSeek）。两者都填效果最佳。"
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
                _sources.value = result?.sources.orEmpty()
                _verified.value = result?.found == true
                refreshBalance()
                _duplicate.value = findDuplicate(result?.title ?: firstLine)

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
                feedUserVocabulary()
                if (!runCatching { searchService.isConfigured() }.getOrDefault(false)) {
                    _error.value = "请先在「设置 → 联网搜索」填入 Tavily 或 Exa 的 Key（负责真实联网检索），并建议在「AI 模型」填入硅基流动 Key（负责先从 OCR 提取准确书名/查询词、再合成最终资料，无需 DeepSeek）。两者都填效果最佳。"
                    _step.value = AddStep.PICK
                    return@launch
                }
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
                _subTags.value = (result?.tags ?: emptyList<String>())
                    .map { TagCatalog.normalize(it) }
                    .toSet()
                _mainTags.value = emptySet()
                _sources.value = result?.sources.orEmpty()
                _verified.value = result?.found == true
                refreshBalance()
                _duplicate.value = findDuplicate(result?.title ?: title)
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
            if (main.size >= 2) { _error.value = "主标签最多 2 个，先取消一个"; return }
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

    /** Promote an existing sub-tag to a main tag (respects the max-2 rule). */
    fun promoteSubToMain(name: String) {
        val main = _mainTags.value.toMutableSet()
        if (main.contains(name)) return
        if (main.size >= 2) { _error.value = "主标签最多 2 个，先取消一个"; return }
        main.add(name)
        _mainTags.value = main
        val sub = _subTags.value.toMutableSet()
        sub.remove(name)
        _subTags.value = sub
    }

    /** Move a main tag back down into the sub-tags. */
    fun demoteMainToSub(name: String) {
        val main = _mainTags.value.toMutableSet()
        if (!main.remove(name)) return
        _mainTags.value = main
        val sub = _subTags.value.toMutableSet()
        sub.add(name)
        _subTags.value = sub
    }

    /**
     * Create a tag the user typed and place it in main- or sub-tags as requested.
     * This is the fix for "custom tags always landed in sub-tags".
     */
    fun addCustomTag(raw: String, asMain: Boolean) {
        val name = raw.trim()
        if (name.isBlank()) return
        viewModelScope.launch { runCatching { repo.upsertTag(Tag(name = name, color = tagColor(name))) } }
        if (asMain) {
            val main = _mainTags.value.toMutableSet()
            if (!main.contains(name) && main.size >= 2) {
                _error.value = "主标签最多 2 个，先取消一个"
                return
            }
            main.add(name)
            _mainTags.value = main
            val sub = _subTags.value.toMutableSet()
            sub.remove(name)
            _subTags.value = sub
        } else {
            val sub = _subTags.value.toMutableSet()
            sub.add(name)
            _subTags.value = sub
        }
    }

    fun toggleWantReRead() { _wantReRead.value = !_wantReRead.value }
    fun toggleWantRecommend() { _wantRecommend.value = !_wantRecommend.value }

    /**
     * Persists the record.
     * - [SaveAction.CREATE] always inserts a new row.
     * - [SaveAction.UPDATE_EXISTING] merges the new data into the duplicate we
     *   detected (non-blank new fields win, tags are unioned, id/add time kept).
     * Returns the row id, or null on failure.
     */
    suspend fun save(action: SaveAction = SaveAction.CREATE): Long? {
        val d = _draft.value
        val finalTitle = d.title.ifBlank { _ocrText.value.lines().firstOrNull { it.isNotBlank() } ?: "" }
        val main = _mainTags.value.filter { it.isNotBlank() }
        val sub = _subTags.value.filter { it.isNotBlank() }
        // Make sure selected tags exist in the catalog so they become filterable on Home.
        (main + sub).forEach { name ->
            runCatching { repo.upsertTag(Tag(name = name, color = tagColor(name))) }
        }

        val existing = _duplicate.value
        if (action == SaveAction.UPDATE_EXISTING && existing != null) {
            val mergedMain = (existing.mainTags + main).distinct().take(2)
            val mergedSub = (existing.subTags + sub).distinct().filter { it !in mergedMain }
            val merged = existing.copy(
                title = finalTitle.ifBlank { existing.title },
                author = d.author.takeIf { it.isNotBlank() } ?: existing.author,
                coverUrl = d.coverUrl.takeIf { it.isNotBlank() } ?: existing.coverUrl,
                synopsis = d.synopsis.takeIf { it.isNotBlank() } ?: existing.synopsis,
                protagonist = d.protagonist.takeIf { it.isNotBlank() } ?: existing.protagonist,
                highlights = d.highlights.takeIf { it.isNotBlank() } ?: existing.highlights,
                wantReRead = _wantReRead.value || existing.wantReRead == true,
                wantRecommend = _wantRecommend.value || existing.wantRecommend == true,
                mainTags = mergedMain,
                subTags = mergedSub,
                source = d.source.takeIf { it.isNotBlank() } ?: existing.source,
                sources = (existing.sources + _sources.value).distinct()
            )
            return runCatching { repo.upsert(merged) }.getOrNull()
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
            source = d.source.ifBlank { null },
            sources = _sources.value
        )
        return runCatching { repo.upsert(novel) }.getOrNull()?.takeIf { id -> id > 0L }
    }

    // ---- helpers ----

    /**
     * Feed the user's own words (tags they created or kept) back to the model so it
     * starts preferring their vocabulary — the "personal dictionary" feedback loop.
     */
    private suspend fun feedUserVocabulary() {
        val custom = runCatching { repo.getAllTags() }.getOrDefault(emptyList())
            .map { it.name }
            .filter { !TagCatalog.isBuiltIn(it) }
        searchService.setUserVocabulary(custom)
    }

    /** Exact title match first, then a conservative prefix/contains match. */
    private suspend fun findDuplicate(title: String): Novel? {
        val key = normalizeTitle(title)
        if (key.isBlank()) return null
        val all = runCatching { repo.getAll() }.getOrDefault(emptyList())
        all.firstOrNull { normalizeTitle(it.title) == key }?.let { return it }
        return all.firstOrNull { n ->
            val k = normalizeTitle(n.title)
            k.isNotBlank() &&
                (k.startsWith(key) || key.startsWith(k)) &&
                (k.length - key.length) in -2..6
        }
    }

    /** Delegates to the shared helper so backup-restore and duplicate check agree. */
    private fun normalizeTitle(t: String): String = BackupManager.normalizeTitle(t)

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
