package com.example.taksy.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.taksy.data.Reminder
import com.example.taksy.data.Subtask
import com.example.taksy.data.Task
import com.example.taksy.data.TaskEstado
import com.example.taksy.data.TaskPrioridad
import com.example.taksy.data.TaskRecurrencia
import com.example.taksy.data.TipoRecordatorio
import com.example.taksy.domain.CompleteTaskUseCase
import com.example.taksy.domain.SubtaskUseCase
import com.example.taksy.domain.TaskReminderUseCase
import com.example.taksy.repository.TaskFilter
import com.example.taksy.repository.TaskRepository
import com.example.taksy.widget.TaskWidgetProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
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
@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val subtaskUseCase: SubtaskUseCase,
    private val reminderUseCase: TaskReminderUseCase,
    application: Application
) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    private val _currentFilter = MutableStateFlow(TaskFilter.TODAS)

    private val togglingTaskIds = mutableSetOf<Long>()

    init {
        viewModelScope.launch {
            _currentFilter
                .flatMapLatest { filter ->
                    _uiState.value = _uiState.value.copy(isLoading = true)
                    repository.getTasksByFilter(filter)
                }
                .collect { tasks ->
                    _uiState.value = _uiState.value.copy(tasks = tasks, isLoading = false)
                }
        }
    }

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
        launchSafe {
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

    fun updateTask(task: Task) = launchSafe { repository.updateTask(task) }

    fun deleteTask(task: Task) = launchSafe {
        repository.deleteTask(task)
        TaskWidgetProvider.refreshAll(context)
    }

    fun restoreTask(task: Task) = launchSafe {
        repository.insertTask(task)
        TaskWidgetProvider.refreshAll(context)
    }

    fun archiveTask(task: Task) = launchSafe {
        repository.archiveTask(task.id)
        TaskWidgetProvider.refreshAll(context)
    }

    fun unarchiveTask(task: Task) = launchSafe {
        repository.unarchiveTask(task.id)
        TaskWidgetProvider.refreshAll(context)
    }

    fun getArchivedTasksByCategory(categoryId: Long): Flow<List<Task>> =
        repository.getArchivedTasksByCategory(categoryId)

    fun reorderTasks(tasks: List<Task>) {
        viewModelScope.launch { repository.reorderTasks(tasks) }
    }

    fun toggleTaskStatus(task: Task) {
        if (!togglingTaskIds.add(task.id)) return
        viewModelScope.launch {
            runCatching {
                if (task.estado == TaskEstado.PENDIENTE) {
                    completeTaskUseCase.execute(task)
                } else {
                    repository.toggleTaskStatus(task)
                }
                TaskWidgetProvider.refreshAll(context)
            }.onFailure { setError(it.message) }
            togglingTaskIds.remove(task.id)
        }
    }

    fun setFilter(filter: TaskFilter) {
        _currentFilter.value = filter
        _uiState.value = _uiState.value.copy(currentFilter = filter)
    }

    fun deleteCompletedTasks() = launchSafe {
        repository.deleteCompletedTasks()
        TaskWidgetProvider.refreshAll(context)
    }

    suspend fun getTaskById(id: Long): Task? = repository.getTaskById(id)

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ── Subtasks (delegated) ───────────────────────────────────────────────

    fun getSubtasksByTaskId(taskId: Long): Flow<List<Subtask>> = subtaskUseCase.observe(taskId)

    fun addSubtask(taskId: Long, titulo: String) = launchSafe { subtaskUseCase.add(taskId, titulo) }

    fun toggleSubtaskStatus(subtask: Subtask) = launchSafe {
        val parentAutoCompleted = subtaskUseCase.toggle(subtask)
        if (parentAutoCompleted) TaskWidgetProvider.refreshAll(context)
    }

    fun deleteSubtask(subtask: Subtask) = launchSafe { subtaskUseCase.delete(subtask) }

    // ── Reminders (delegated) ──────────────────────────────────────────────

    fun getRemindersByTaskId(taskId: Long): Flow<List<Reminder>> = reminderUseCase.observe(taskId)

    fun getAllActiveReminders(): Flow<List<Reminder>> = reminderUseCase.observeAllActive()

    fun addReminder(
        taskId: Long,
        titulo: String,
        descripcion: String?,
        fechaRecordatorio: Date,
        tipoRecordatorio: TipoRecordatorio
    ) = launchSafe { reminderUseCase.add(taskId, titulo, descripcion, fechaRecordatorio, tipoRecordatorio) }

    fun updateReminder(reminder: Reminder) = launchSafe { reminderUseCase.update(reminder) }

    fun deleteReminder(reminder: Reminder) = launchSafe { reminderUseCase.delete(reminder) }

    fun updateReminderStatus(id: Long, activo: Boolean) = launchSafe { reminderUseCase.setActive(id, activo) }

    fun setQuickReminder(taskId: Long, taskTitle: String, fecha: Date) =
        launchSafe { reminderUseCase.setQuick(taskId, taskTitle, fecha) }

    fun deleteReminderForTask(taskId: Long) = launchSafe { reminderUseCase.deleteAllForTask(taskId) }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun launchSafe(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }.onFailure { setError(it.message) }
        }
    }

    private fun setError(message: String?) {
        _uiState.value = _uiState.value.copy(error = message ?: "Error desconocido")
    }
}

data class TaskUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = true,
    val currentFilter: TaskFilter = TaskFilter.TODAS,
    val error: String? = null
)
