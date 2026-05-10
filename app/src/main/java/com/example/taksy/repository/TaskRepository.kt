package com.example.taksy.repository

import com.example.taksy.data.Reminder
import com.example.taksy.data.ReminderDao
import com.example.taksy.data.Subtask
import com.example.taksy.data.SubtaskDao
import com.example.taksy.data.Task
import com.example.taksy.data.TaskDao
import com.example.taksy.data.TaskEstado
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject

class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val subtaskDao: SubtaskDao,
    private val reminderDao: ReminderDao
) {
    // ── Tasks ──────────────────────────────────────────────────────────────

    fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()
    fun getPendingTasks(): Flow<List<Task>> = taskDao.getPendingTasks()
    fun getCompletedTasks(): Flow<List<Task>> = taskDao.getCompletedTasks()
    fun getTasksByCategoryId(categoryId: Long): Flow<List<Task>> = taskDao.getTasksByCategory(categoryId)
    fun searchTasksByCategory(categoryId: Long, query: String): Flow<List<Task>> =
        taskDao.searchTasksByCategory(categoryId, query)
    fun searchAllTasks(query: String): Flow<List<Task>> =
        taskDao.searchAllTasks(query)

    fun getTasksByFilter(filter: TaskFilter): Flow<List<Task>> = when (filter) {
        TaskFilter.TODAS -> getAllTasks()
        TaskFilter.PENDIENTES -> taskDao.getReallyPendingTasks()
        TaskFilter.COMPLETADAS -> taskDao.getReallyCompletedTasks()
    }

    suspend fun getTaskById(id: Long): Task? = taskDao.getTaskById(id)
    suspend fun insertTask(task: Task): Long = taskDao.insertTask(task)
    suspend fun updateTask(task: Task) = taskDao.updateTask(task)
    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)
    suspend fun deleteCompletedTasks() = taskDao.deleteCompletedTasks()

    fun getArchivedTasksByCategory(categoryId: Long): Flow<List<Task>> =
        taskDao.getArchivedTasksByCategory(categoryId)
    suspend fun archiveTask(taskId: Long) = taskDao.archiveTask(taskId)
    suspend fun unarchiveTask(taskId: Long) = taskDao.unarchiveTask(taskId)
    suspend fun reorderTasks(tasks: List<Task>) = taskDao.updateTasks(tasks)

    suspend fun toggleTaskStatus(task: Task) {
        val completing = task.estado == TaskEstado.PENDIENTE
        val newStatus = if (completing) TaskEstado.COMPLETADA else TaskEstado.PENDIENTE
        taskDao.updateTask(task.copy(
            estado = newStatus,
            fechaCompletada = if (completing) Date() else null
        ))
    }

    // ── Subtasks ───────────────────────────────────────────────────────────

    fun getSubtasksByTaskId(taskId: Long): Flow<List<Subtask>> = subtaskDao.getSubtasksByTaskId(taskId)
    suspend fun insertSubtask(subtask: Subtask): Long = subtaskDao.insertSubtask(subtask)
    suspend fun updateSubtask(subtask: Subtask) = subtaskDao.updateSubtask(subtask)
    suspend fun deleteSubtask(subtask: Subtask) = subtaskDao.deleteSubtask(subtask)
    suspend fun deleteSubtasksByTaskId(taskId: Long) = subtaskDao.deleteSubtasksByTaskId(taskId)

    // ── Reminders ──────────────────────────────────────────────────────────

    fun getRemindersByTaskId(taskId: Long): Flow<List<Reminder>> = reminderDao.getRemindersByTaskId(taskId)
    fun getAllActiveReminders(): Flow<List<Reminder>> = reminderDao.getAllActiveReminders()
    suspend fun getReminderById(id: Long): Reminder? = reminderDao.getReminderById(id)
    suspend fun insertReminder(reminder: Reminder): Long = reminderDao.insertReminder(reminder)
    suspend fun updateReminder(reminder: Reminder) = reminderDao.updateReminder(reminder)
    suspend fun deleteReminder(reminder: Reminder) = reminderDao.deleteReminder(reminder)
    suspend fun deleteRemindersByTaskId(taskId: Long) = reminderDao.deleteRemindersByTaskId(taskId)
    suspend fun updateReminderStatus(id: Long, activo: Boolean) = reminderDao.updateReminderStatus(id, activo)
    suspend fun getRemindersByTaskIdSync(taskId: Long): List<Reminder> = reminderDao.getRemindersByTaskIdSync(taskId)

    suspend fun cancelAllRemindersForTask(taskId: Long) {
        reminderDao.getRemindersByTaskIdSync(taskId).forEach { reminder ->
            reminderDao.updateReminder(reminder.copy(activo = false))
        }
    }
}

enum class TaskFilter { TODAS, PENDIENTES, COMPLETADAS }
