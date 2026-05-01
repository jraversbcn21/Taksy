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
import com.example.taksy.data.TipoRecordatorio
import com.example.taksy.repository.TaskFilter
import com.example.taksy.repository.TaskRepository
import com.example.taksy.service.ReminderScheduler
import com.example.taksy.widget.TaskWidgetProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModel @Inject constructor(
    private val repository: TaskRepository,
    application: Application
) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    private val _currentFilter = MutableStateFlow(TaskFilter.TODAS)

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

    fun addTask(titulo: String) = addTask(titulo, null, null, TaskPrioridad.NINGUNA)

    fun addTask(titulo: String, fechaVencimiento: Date?) = addTask(titulo, fechaVencimiento, null, TaskPrioridad.NINGUNA)

    fun addTask(titulo: String, fechaVencimiento: Date?, categoriaId: Long?) = addTask(titulo, fechaVencimiento, categoriaId, TaskPrioridad.NINGUNA)

    fun addTask(titulo: String, fechaVencimiento: Date?, categoriaId: Long?, prioridad: TaskPrioridad) {
        if (titulo.isBlank()) return
        viewModelScope.launch {
            runCatching {
                repository.insertTask(Task(titulo = titulo.trim(), fechaVencimiento = fechaVencimiento, categoriaId = categoriaId, prioridad = prioridad))
                TaskWidgetProvider.refreshAll(context)
            }.onFailure { setError(it.message) }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch { runCatching { repository.updateTask(task) }.onFailure { setError(it.message) } }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            runCatching {
                repository.deleteTask(task)
                TaskWidgetProvider.refreshAll(context)
            }.onFailure { setError(it.message) }
        }
    }

    fun toggleTaskStatus(task: Task) {
        viewModelScope.launch {
            runCatching {
                if (task.estado == TaskEstado.PENDIENTE) {
                    val scheduler = ReminderScheduler(context)
                    repository.getRemindersByTaskIdSync(task.id).forEach { scheduler.cancelReminder(it.id) }
                    repository.cancelAllRemindersForTask(task.id)
                }
                repository.toggleTaskStatus(task)
                TaskWidgetProvider.refreshAll(context)
            }.onFailure { setError(it.message) }
        }
    }

    fun setFilter(filter: TaskFilter) {
        _currentFilter.value = filter
        _uiState.value = _uiState.value.copy(currentFilter = filter)
    }

    fun deleteCompletedTasks() {
        viewModelScope.launch {
            runCatching {
                repository.deleteCompletedTasks()
                TaskWidgetProvider.refreshAll(context)
            }.onFailure { setError(it.message) }
        }
    }

    suspend fun getTaskById(id: Long): Task? = repository.getTaskById(id)

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ── Subtasks ───────────────────────────────────────────────────────────

    fun getSubtasksByTaskId(taskId: Long): Flow<List<Subtask>> = repository.getSubtasksByTaskId(taskId)

    fun addSubtask(taskId: Long, titulo: String) {
        if (titulo.isBlank()) return
        viewModelScope.launch {
            runCatching { repository.insertSubtask(Subtask(taskId = taskId, titulo = titulo.trim())) }
                .onFailure { setError(it.message) }
        }
    }

    fun toggleSubtaskStatus(subtask: Subtask) {
        viewModelScope.launch {
            runCatching {
                val updated = subtask.copy(
                    estado = if (subtask.estado == TaskEstado.PENDIENTE) TaskEstado.COMPLETADA else TaskEstado.PENDIENTE
                )
                repository.updateSubtask(updated)

                // Auto-complete parent task when all subtasks are done
                val subtasks = repository.getSubtasksByTaskId(subtask.taskId).first()
                if (subtasks.isNotEmpty() && subtasks.all { it.estado == TaskEstado.COMPLETADA }) {
                    val task = repository.getTaskById(subtask.taskId)
                    if (task != null && task.estado == TaskEstado.PENDIENTE) {
                        repository.updateTask(task.copy(estado = TaskEstado.COMPLETADA))
                        TaskWidgetProvider.refreshAll(context)
                    }
                }
            }.onFailure { setError(it.message) }
        }
    }

    fun deleteSubtask(subtask: Subtask) {
        viewModelScope.launch { runCatching { repository.deleteSubtask(subtask) }.onFailure { setError(it.message) } }
    }

    // ── Reminders ──────────────────────────────────────────────────────────

    fun getRemindersByTaskId(taskId: Long): Flow<List<Reminder>> = repository.getRemindersByTaskId(taskId)

    fun getAllActiveReminders(): Flow<List<Reminder>> = repository.getAllActiveReminders()

    fun addReminder(
        taskId: Long,
        titulo: String,
        descripcion: String?,
        fechaRecordatorio: Date,
        tipoRecordatorio: TipoRecordatorio
    ) {
        viewModelScope.launch {
            runCatching {
                val reminder = Reminder(
                    taskId = taskId,
                    titulo = titulo,
                    descripcion = descripcion,
                    fechaRecordatorio = fechaRecordatorio,
                    tipoRecordatorio = tipoRecordatorio
                )
                val id = repository.insertReminder(reminder)
                ReminderScheduler(context).scheduleReminder(reminder.copy(id = id))
            }.onFailure { setError(it.message) }
        }
    }

    fun updateReminder(reminder: Reminder) {
        viewModelScope.launch { runCatching { repository.updateReminder(reminder) }.onFailure { setError(it.message) } }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch { runCatching { repository.deleteReminder(reminder) }.onFailure { setError(it.message) } }
    }

    fun updateReminderStatus(id: Long, activo: Boolean) {
        viewModelScope.launch { runCatching { repository.updateReminderStatus(id, activo) }.onFailure { setError(it.message) } }
    }

    /**
     * Sets a single quick reminder for a task, replacing any existing ones.
     */
    fun setQuickReminder(taskId: Long, taskTitle: String, fecha: Date) {
        viewModelScope.launch {
            runCatching {
                val scheduler = ReminderScheduler(context)
                // Remove existing reminders for this task
                repository.getRemindersByTaskIdSync(taskId).forEach {
                    scheduler.cancelReminder(it.id)
                    repository.deleteReminder(it)
                }
                // Create new one
                val reminder = Reminder(
                    taskId = taskId,
                    titulo = taskTitle,
                    fechaRecordatorio = fecha,
                    tipoRecordatorio = TipoRecordatorio.UNA_VEZ
                )
                val id = repository.insertReminder(reminder)
                scheduler.scheduleReminder(reminder.copy(id = id))
            }.onFailure { setError(it.message) }
        }
    }

    /**
     * Deletes all reminders for a task.
     */
    fun deleteReminderForTask(taskId: Long) {
        viewModelScope.launch {
            runCatching {
                val scheduler = ReminderScheduler(context)
                repository.getRemindersByTaskIdSync(taskId).forEach {
                    scheduler.cancelReminder(it.id)
                    repository.deleteReminder(it)
                }
            }.onFailure { setError(it.message) }
        }
    }

    // ─��� Helpers ─────────────────────���──────────────────────────���───────────

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
