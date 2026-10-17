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
import com.example.taksy.domain.SubtaskUseCase
import com.example.taksy.domain.TaskReminderUseCase
import com.example.taksy.repository.TaskRepository
import com.example.taksy.widget.TaskWidgetProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

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

    fun deleteTask(task: Task) = launchSafe(R.string.error_delete_task) {
        repository.deleteTask(task)
        TaskWidgetProvider.refreshAll(context)
    }

    fun restoreTask(task: Task) = launchSafe(R.string.error_add_task) {
        repository.insertTask(task)
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

    fun setQuickReminder(taskId: Long, taskTitle: String, fecha: Date) =
        launchSafe(R.string.error_add_reminder) { reminderUseCase.setQuick(taskId, taskTitle, fecha) }

    fun deleteReminderForTask(taskId: Long) = launchSafe(R.string.error_delete_reminder) { reminderUseCase.deleteAllForTask(taskId) }

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
