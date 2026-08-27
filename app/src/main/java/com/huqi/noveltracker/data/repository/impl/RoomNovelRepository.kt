package com.huqi.noveltracker.data.repository.impl

import com.huqi.noveltracker.data.local.NovelDao
import com.huqi.noveltracker.data.local.TagDao
import com.huqi.noveltracker.data.local.entity.toEntity
import com.huqi.noveltracker.data.local.entity.toModel
import com.huqi.noveltracker.data.model.Novel
import com.huqi.noveltracker.data.model.Tag
import com.huqi.noveltracker.data.repository.NovelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomNovelRepository(
    private val novelDao: NovelDao,
    private val tagDao: TagDao
) : NovelRepository {

    override fun observeNovels(): Flow<List<Novel>> =
        novelDao.observeAll().map { list -> list.map { it.toModel() } }

    override fun observeNovel(id: Long): Flow<Novel?> =
        novelDao.observeById(id).map { it?.toModel() }

    override fun observeTags(): Flow<List<Tag>> =
        tagDao.observeAll().map { list -> list.map { it.toModel() } }

    override suspend fun upsert(novel: Novel): Long = novelDao.upsertAndGetId(novel.toEntity())
    override suspend fun delete(novel: Novel) = novelDao.delete(novel.toEntity())

    override suspend fun upsertTag(tag: Tag) = tagDao.upsert(tag.toEntity())
    override suspend fun deleteTag(tag: Tag) = tagDao.delete(tag.toEntity())
}
