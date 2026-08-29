package com.huqi.noveltracker.ui.screen.backup

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.huqi.noveltracker.appContainer
import com.huqi.noveltracker.util.BackupManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BackupViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = application.appContainer.novelRepository

    val novelCount: StateFlow<Int> = repo.observeNovels()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() { _message.value = null }

    /** Serialize the library and hand it to the system share sheet. */
    fun export(context: Context) = viewModelScope.launch {
        val novels = runCatching { repo.getAll() }.getOrDefault(emptyList())
        val tags = runCatching { repo.getAllTags() }.getOrDefault(emptyList())
        val uri = BackupManager.exportJson(context, novels, tags)
        if (uri != null) {
            BackupManager.share(context, uri)
            _message.value = "已生成备份：${novels.size} 本书、${tags.size} 个标签"
        } else {
            _message.value = "书架是空的，没有可导出的内容"
        }
    }

    /** Merge a previously exported file back in — never deletes existing rows. */
    fun restore(context: Context, uri: Uri) = viewModelScope.launch {
        val backup = BackupManager.readBackup(context, uri)
        if (backup == null) {
            _message.value = "这个文件读不出来，确认选的是书架备份的 JSON 吗？"
            return@launch
        }
        if (backup.novels.isEmpty() && backup.tags.isEmpty()) {
            _message.value = "备份文件里没有可恢复的内容"
            return@launch
        }
        backup.tags.forEach { tag -> runCatching { repo.upsertTag(tag) } }

        // Match by normalized title so restoring twice doesn't create duplicates:
        // an existing book gets updated in place, a new one is inserted.
        val existing = runCatching { repo.getAll() }.getOrDefault(emptyList())
        val byTitle = existing.associateBy { BackupManager.normalizeTitle(it.title) }
        var restored = 0
        var updated = 0
        backup.novels.forEach { novel ->
            val match = byTitle[BackupManager.normalizeTitle(novel.title)]
            val target = if (match != null) {
                novel.copy(id = match.id, addedAt = match.addedAt)
            } else novel
            val id = runCatching { repo.upsert(target) }.getOrDefault(0L)
            if (id > 0L) {
                restored++
                if (match != null) updated++
            }
        }
        val tail = if (updated > 0) "（其中 $updated 本已存在，已更新）" else ""
        _message.value = "已恢复 $restored 本书、${backup.tags.size} 个标签$tail"
    }
}
