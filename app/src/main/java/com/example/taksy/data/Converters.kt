package com.example.taksy.data

import androidx.room.TypeConverter
import java.util.Date

/**
 * Convertidores de tipos para Room Database
 */
class Converters {
    
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
