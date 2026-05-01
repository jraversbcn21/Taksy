package com.example.taksy.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taksy.data.*
import com.example.taksy.service.ReminderScheduler
import com.example.taksy.utils.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskDao: TaskDao,
    private val subtaskDao: SubtaskDao,
    private val categoryDao: CategoryDao,
    private val reminderDao: ReminderDao
) : ViewModel() {

    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState.asStateFlow()

    fun generateBackupJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            _backupState.value = BackupState.Loading
            try {
                val data = BackupManager.BackupData(
                    categories = categoryDao.getAllCategoriesSync(),
                    tasks = taskDao.getAllTasksSync(),
                    subtasks = subtaskDao.getAllSubtasksSync(),
                    reminders = reminderDao.getAllRemindersSync()
                )
                val json = BackupManager.exportToJson(data)
                _backupState.value = BackupState.ExportSuccess
                onResult(json)
            } catch (e: Exception) {
                _backupState.value = BackupState.Error(e.message ?: "Export error")
            }
        }
    }

    fun importBackupJson(json: String) {
        viewModelScope.launch {
            _backupState.value = BackupState.Loading
            try {
                val data = BackupManager.importFromJson(json)

                reminderDao.deleteAllReminders()
                subtaskDao.deleteAllSubtasks()
                taskDao.deleteAllTasks()
                categoryDao.deleteAllCategories()

                categoryDao.insertCategories(data.categories)
                data.tasks.forEach { taskDao.insertTask(it) }
                data.subtasks.forEach { subtaskDao.insertSubtask(it) }
                data.reminders.forEach { reminderDao.insertReminder(it) }

                val scheduler = ReminderScheduler(context)
                val now = Date()
                data.reminders.filter { it.activo && it.fechaRecordatorio.after(now) }
                    .forEach { scheduler.scheduleReminder(it) }

                _backupState.value = BackupState.ImportSuccess(
                    categories = data.categories.size,
                    tasks = data.tasks.size,
                    subtasks = data.subtasks.size,
                    reminders = data.reminders.size
                )
            } catch (e: BackupManager.BackupImportException) {
                _backupState.value = BackupState.ImportError(e.errorType, e.detail)
            } catch (e: Exception) {
                _backupState.value = BackupState.Error(e.message ?: "Import error")
            }
        }
    }

    fun resetState() {
        _backupState.value = BackupState.Idle
    }
}

sealed class BackupState {
    data object Idle : BackupState()
    data object Loading : BackupState()
    data object ExportSuccess : BackupState()
    data class ImportSuccess(
        val categories: Int,
        val tasks: Int,
        val subtasks: Int,
        val reminders: Int
    ) : BackupState()
    data class ImportError(
        val errorType: BackupManager.ImportErrorType,
        val detail: String?
    ) : BackupState()
    data class Error(val message: String) : BackupState()
}
