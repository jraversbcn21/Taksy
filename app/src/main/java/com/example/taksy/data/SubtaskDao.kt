package com.example.taksy.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones de subtareas
 */
@Dao
interface SubtaskDao {
    
    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY id ASC")
    fun getSubtasksByTaskId(taskId: Long): Flow<List<Subtask>>
    
    @Insert
    suspend fun insertSubtask(subtask: Subtask): Long
    
    @Update
    suspend fun updateSubtask(subtask: Subtask)
    
    @Delete
    suspend fun deleteSubtask(subtask: Subtask)
    
    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    suspend fun deleteSubtasksByTaskId(taskId: Long)

    @Query("SELECT * FROM subtasks ORDER BY id ASC")
    suspend fun getAllSubtasksSync(): List<Subtask>

    @Query("DELETE FROM subtasks")
    suspend fun deleteAllSubtasks()
}
