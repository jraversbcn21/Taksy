package com.example.taksy.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.taksy.R
import com.example.taksy.data.Reminder
import com.example.taksy.data.Subtask
import com.example.taksy.data.Task
import com.example.taksy.data.TaskEstado
import com.example.taksy.data.TaskPrioridad
import com.example.taksy.data.TaskRecurrencia
import com.example.taksy.domain.CompleteTaskUseCase
import com.example.taksy.domain.DeleteTaskUseCase
import com.example.taksy.domain.SubtaskUseCase
import com.example.taksy.domain.ReminderLead
import com.example.taksy.domain.TaskReminderUseCase
import com.example.taksy.repository.DeletedTask
import com.example.taksy.repository.TaskRepository
import com.example.taksy.widget.TaskWidgetProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class TaskInput(
    val titulo: String,
    val fechaVencimiento: Date? = null,
    val categoriaId: Long? = null,
    val prioridad: TaskPrioridad = TaskPrioridad.NINGUNA,
    val recurrencia: TaskRecurrencia = TaskRecurrencia.NINGUNA
)

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val subtaskUseCase: SubtaskUseCase,
    private val reminderUseCase: TaskReminderUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    application: Application
) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext

    private val _errors = Channel<Int>(Channel.BUFFERED)

    /** Errores de operaciones, como id de string. Evento de un solo uso: lo muestra MainActivity. */
    val errors: Flow<Int> = _errors.receiveAsFlow()

    private val togglingTaskIds = mutableSetOf<Long>()

    // ── Tasks ──────────────────────────────────────────────────────────────

    fun getAllTasks(): Flow<List<Task>> = repository.getAllTasks()

    fun getTasksByCategoryId(categoryId: Long): Flow<List<Task>> =
        repository.getTasksByCategoryId(categoryId)

    fun searchTasksByCategory(categoryId: Long, query: String): Flow<List<Task>> =
        repository.searchTasksByCategory(categoryId, query)

    fun searchAllTasks(query: String): Flow<List<Task>> =
        repository.searchAllTasks(query)

    fun addTask(input: TaskInput) {
        if (input.titulo.isBlank()) return
        launchSafe(R.string.error_add_task) {
            repository.insertTask(
                Task(
                    titulo = input.titulo.trim(),
                    fechaVencimiento = input.fechaVencimiento,
                    categoriaId = input.categoriaId,
                    prioridad = input.prioridad,
                    recurrencia = input.recurrencia
                )
            )
            TaskWidgetProvider.refreshAll(context)
        }
    }

    fun updateTask(task: Task) = launchSafe(R.string.error_update_task) { repository.updateTask(task) }

    // Lo borrado se guarda hasta que se deshace, para restaurar también subtareas y recordatorios.
    // El mutex hace que un "Deshacer" rápido espere a que termine el borrado.
    private val recentlyDeleted = mutableMapOf<Long, DeletedTask>()
    private val deleteMutex = Mutex()

    fun deleteTask(task: Task) = launchSafe(R.string.error_delete_task) {
        deleteMutex.withLock {
            recentlyDeleted[task.id] = deleteTaskUseCase.delete(task)
        }
        TaskWidgetProvider.refreshAll(context)
    }

    fun restoreTask(task: Task) = launchSafe(R.string.error_add_task) {
        deleteMutex.withLock {
            val deleted = recentlyDeleted.remove(task.id) ?: return@withLock
            deleteTaskUseCase.restore(deleted)
        }
        TaskWidgetProvider.refreshAll(context)
    }

    fun toggleTaskStatus(task: Task) {
        if (!togglingTaskIds.add(task.id)) return
        launchSafe(R.string.error_toggle_task) {
            try {
                if (task.estado == TaskEstado.PENDIENTE) {
                    completeTaskUseCase.execute(task)
                } else {
                    repository.markTaskPending(task.id)
                }
                TaskWidgetProvider.refreshAll(context)
            } finally {
                togglingTaskIds.remove(task.id)
            }
        }
    }

    // ── Subtasks (delegated) ───────────────────────────────────────────────

    fun getSubtasksByTaskId(taskId: Long): Flow<List<Subtask>> = subtaskUseCase.observe(taskId)

    fun addSubtask(taskId: Long, titulo: String) = launchSafe(R.string.error_add_subtask) { subtaskUseCase.add(taskId, titulo) }

    fun toggleSubtaskStatus(subtask: Subtask) = launchSafe(R.string.error_update_subtask) {
        val parentAutoCompleted = subtaskUseCase.toggle(subtask)
        if (parentAutoCompleted) TaskWidgetProvider.refreshAll(context)
    }

    fun deleteSubtask(subtask: Subtask) = launchSafe(R.string.error_delete_subtask) { subtaskUseCase.delete(subtask) }

    // ── Reminders (delegated) ──────────────────────────────────────────────

    fun getRemindersByTaskId(taskId: Long): Flow<List<Reminder>> = reminderUseCase.observe(taskId)

    /** Aviso respecto a la fecha de la tarea; null lo quita. */
    fun setReminderLead(task: Task, lead: ReminderLead?) =
        launchSafe(if (lead == null) R.string.error_delete_reminder else R.string.error_add_reminder) {
            reminderUseCase.setLead(task, lead)
        }

    /** Cambia la fecha de vencimiento; el aviso se mueve con ella (TaskReminderUseCase.updateDueDate). */
    fun updateDueDate(task: Task, newDue: Date?) = launchSafe(R.string.error_update_task) {
        reminderUseCase.updateDueDate(task, newDue)
        TaskWidgetProvider.refreshAll(context)
    }

    /** Fecha y aviso a la vez (diálogo de la campana de la lista). */
    fun setSchedule(task: Task, newDue: Date?, lead: ReminderLead?) = launchSafe(R.string.error_add_reminder) {
        reminderUseCase.updateDueDate(task, newDue)
        reminderUseCase.setLead(task.copy(fechaVencimiento = newDue), lead)
        TaskWidgetProvider.refreshAll(context)
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun launchSafe(@StringRes errorMessage: Int, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Task operation failed", e)
                _errors.trySend(errorMessage)
            }
        }
    }

    private companion object {
        const val TAG = "TaskViewModel"
    }
}
