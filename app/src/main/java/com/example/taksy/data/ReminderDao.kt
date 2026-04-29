package com.example.taksy.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones de recordatorios
 */
@Dao
interface ReminderDao {
    
    @Query("SELECT * FROM reminders WHERE taskId = :taskId ORDER BY fechaRecordatorio ASC")
    fun getRemindersByTaskId(taskId: Long): Flow<List<Reminder>>
    
    @Query("SELECT * FROM reminders WHERE activo = 1 AND fechaRecordatorio <= :now ORDER BY fechaRecordatorio ASC")
    suspend fun getActiveRemindersDue(now: Long): List<Reminder>
    
    @Query("SELECT * FROM reminders WHERE activo = 1 ORDER BY fechaRecordatorio ASC")
    fun getAllActiveReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE activo = 1 ORDER BY fechaRecordatorio ASC")
    suspend fun getAllActiveRemindersSync(): List<Reminder>
    
    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): Reminder?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long
    
    @Update
    suspend fun updateReminder(reminder: Reminder)
    
    @Delete
    suspend fun deleteReminder(reminder: Reminder)
    
    @Query("DELETE FROM reminders WHERE taskId = :taskId")
    suspend fun deleteRemindersByTaskId(taskId: Long)
    
    @Query("UPDATE reminders SET activo = :activo WHERE id = :id")
    suspend fun updateReminderStatus(id: Long, activo: Boolean)
    
    @Query("SELECT * FROM reminders WHERE taskId = :taskId")
    suspend fun getRemindersByTaskIdSync(taskId: Long): List<Reminder>

    @Query("SELECT * FROM reminders ORDER BY id ASC")
    suspend fun getAllRemindersSync(): List<Reminder>

    @Query("DELETE FROM reminders")
    suspend fun deleteAllReminders()
}
