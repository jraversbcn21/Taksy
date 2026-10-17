package com.example.taksy.repository

import androidx.room.withTransaction
import com.example.taksy.data.AppDatabase
import com.example.taksy.data.Reminder
import com.example.taksy.data.ReminderDao
import com.example.taksy.data.Subtask
import com.example.taksy.data.SubtaskDao
import com.example.taksy.data.Task
import com.example.taksy.data.TaskDao
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject

class TaskRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val taskDao: TaskDao,
    private val subtaskDao: SubtaskDao,
    private val reminderDao: ReminderDao
) {
    // ── Tasks ──────────────────────────────────────────────────────────────

    fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()
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
    /**
     * Borra una tarea con sus subtareas y recordatorios en una sola transacción y devuelve
     * lo borrado, para poder deshacerlo con [restoreDeletedTask] y cancelar las alarmas.
     */
    suspend fun deleteTaskWithChildren(task: Task): DeletedTask =
        appDatabase.withTransaction {
            val deleted = DeletedTask(
                task = task,
                subtasks = subtaskDao.getSubtasksByTaskIdSync(task.id),
                reminders = reminderDao.getRemindersByTaskIdSync(task.id)
            )
            reminderDao.deleteRemindersByTaskId(task.id)
            subtaskDao.deleteSubtasksByTaskId(task.id)
            taskDao.deleteTask(task)
            deleted
        }

    /** Vuelve a insertar una tarea borrada con sus subtareas y recordatorios, conservando los ids. */
    suspend fun restoreDeletedTask(deleted: DeletedTask) =
        appDatabase.withTransaction {
            taskDao.insertTask(deleted.task)
            deleted.subtasks.forEach { subtaskDao.insertSubtask(it) }
            deleted.reminders.forEach { reminderDao.insertReminder(it) }
        }


    /**
     * Completa la tarea solo si sigue PENDIENTE en la base de datos (compare-and-swap).
     * Devuelve true si esta llamada fue la que la completó.
     */
    suspend fun completeTaskIfPending(taskId: Long): Boolean =
        taskDao.completeTaskIfPending(taskId, Date()) == 1

    suspend fun markTaskPending(taskId: Long) = taskDao.markTaskAsPending(taskId)

    /**
     * Completa una tarea recurrente e inserta su siguiente ocurrencia de forma atómica.
     * El compare-and-swap dentro de la transacción garantiza que solo el primer caller
     * inserta el clon: cualquier ejecución concurrente ve estado != PENDIENTE y no escribe.
     * Los [cloneReminders] se insertan asociados al clon en la misma transacción.
     * Devuelve los recordatorios insertados (con su id) si esta llamada completó la tarea,
     * o null si otro caller ya la había completado.
     */
    suspend fun completeRecurringTask(
        task: Task,
        clone: Task,
        cloneReminders: List<Reminder> = emptyList()
    ): List<Reminder>? =
        appDatabase.withTransaction {
            if (taskDao.completeTaskIfPending(task.id, Date()) != 1) return@withTransaction null
            val cloneId = taskDao.insertTask(clone)
            cloneReminders.map { template ->
                val reminder = template.copy(id = 0, taskId = cloneId, activo = true)
                reminder.copy(id = reminderDao.insertReminder(reminder))
            }
        }

    /**
     * Borra todas las tareas de una categoría junto con sus subtareas y recordatorios,
     * en una sola transacción. Devuelve los ids de los recordatorios borrados para que
     * el caller cancele sus alarmas.
     */
    suspend fun deleteTasksInCategory(categoryId: Long): List<Long> =
        appDatabase.withTransaction {
            taskDao.getAllTasksByCategorySync(categoryId).flatMap { task ->
                val reminderIds = reminderDao.getRemindersByTaskIdSync(task.id).map { it.id }
                reminderDao.deleteRemindersByTaskId(task.id)
                subtaskDao.deleteSubtasksByTaskId(task.id)
                taskDao.deleteTask(task)
                reminderIds
            }
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

    suspend fun cancelAllRemindersForTask(taskId: Long) =
        reminderDao.deactivateRemindersByTaskId(taskId)
}

/** Una tarea borrada junto con sus subtareas y recordatorios, para poder deshacer el borrado. */
data class DeletedTask(val task: Task, val subtasks: List<Subtask>, val reminders: List<Reminder>)

enum class TaskFilter { TODAS, PENDIENTES, COMPLETADAS }
