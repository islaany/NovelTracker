package com.huqi.noveltracker.data.local

import androidx.room.TypeConverter

/**
 * Stores the novel's tag list (List<String>) as a pipe-delimited string.
 * Simple on purpose for the framework; can be upgraded to a join table later.
 */
class Converters {
    @TypeConverter
    fun fromTagList(list: List<String>): String = list.joinToString("|")

    @TypeConverter
    fun toTagList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split("|")
}
