package com.huqi.noveltracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.huqi.noveltracker.data.model.Tag

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val name: String,
    val color: String
)

fun TagEntity.toModel(): Tag = Tag(name = name, color = color)
fun Tag.toEntity(): TagEntity = TagEntity(name = name, color = color)
