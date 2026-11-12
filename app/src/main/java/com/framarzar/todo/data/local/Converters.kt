package com.framarzar.todo.data.local

import androidx.room.TypeConverter

private const val TAG_SEPARATOR = ","

class Converters {
    @TypeConverter
    fun tagsToString(tags: List<String>): String = tags.joinToString(TAG_SEPARATOR)

    @TypeConverter
    fun stringToTags(value: String): List<String> =
        value.split(TAG_SEPARATOR).filter { it.isNotBlank() }
}
