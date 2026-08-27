package com.huqi.noveltracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.huqi.noveltracker.data.local.Converters
import com.huqi.noveltracker.data.model.Novel

@Entity(tableName = "novels")
data class NovelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String?,
    val coverUrl: String?,
    val synopsis: String?,
    val protagonist: String?,
    val highlights: String?,
    val wantReRead: Boolean?,
    val wantRecommend: Boolean?,
    val tags: List<String>,
    val source: String?,
    val addedAt: Long
)

fun NovelEntity.toModel(): Novel = Novel(
    id = id,
    title = title,
    author = author,
    coverUrl = coverUrl,
    synopsis = synopsis,
    protagonist = protagonist,
    highlights = highlights,
    wantReRead = wantReRead,
    wantRecommend = wantRecommend,
    tags = tags,
    source = source,
    addedAt = addedAt
)

fun Novel.toEntity(): NovelEntity = NovelEntity(
    id = id,
    title = title,
    author = author,
    coverUrl = coverUrl,
    synopsis = synopsis,
    protagonist = protagonist,
    highlights = highlights,
    wantReRead = wantReRead,
    wantRecommend = wantRecommend,
    tags = tags,
    source = source,
    addedAt = addedAt
)
