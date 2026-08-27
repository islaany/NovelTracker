package com.huqi.noveltracker.data.repository

import com.huqi.noveltracker.data.model.Novel
import com.huqi.noveltracker.data.model.Tag
import kotlinx.coroutines.flow.Flow

/**
 * Local persistence for novels and the tag catalog.
 * Implemented by RoomNovelRepository. Swap for a cloud-backed impl later.
 */
interface NovelRepository {
    fun observeNovels(): Flow<List<Novel>>
    fun observeNovel(id: Long): Flow<Novel?>
    fun observeTags(): Flow<List<Tag>>

    suspend fun upsert(novel: Novel): Long
    suspend fun delete(novel: Novel)

    suspend fun upsertTag(tag: Tag)
    suspend fun deleteTag(tag: Tag)
}
