package com.example.taksy.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

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

    @Query("UPDATE reminders SET activo = 0 WHERE taskId = :taskId")
    suspend fun deactivateRemindersByTaskId(taskId: Long)

    /**
     * Avanza la fecha de un recordatorio recurrente solo si sigue activo y su fecha
     * no ha cambiado desde que se leyó (compare-and-swap).
     * Devuelve 1 si esta llamada ganó, 0 si otro caller ya lo avanzó, lo editó o lo desactivó.
     */
    @Query("UPDATE reminders SET fechaRecordatorio = :nextDate WHERE id = :id AND fechaRecordatorio = :expectedDate AND activo = 1")
    suspend fun advanceReminderIfUnchanged(id: Long, expectedDate: Date, nextDate: Date): Int

    @Query("SELECT * FROM reminders WHERE taskId = :taskId")
    suspend fun getRemindersByTaskIdSync(taskId: Long): List<Reminder>

    @Query("SELECT * FROM reminders ORDER BY id ASC")
    suspend fun getAllRemindersSync(): List<Reminder>

    @Query("DELETE FROM reminders")
    suspend fun deleteAllReminders()
}
