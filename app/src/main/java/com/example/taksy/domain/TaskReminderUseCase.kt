package com.example.taksy.domain

import com.example.taksy.data.Reminder
import com.example.taksy.data.TipoRecordatorio
import com.example.taksy.repository.TaskRepository
import com.example.taksy.service.ReminderSchedulerContract
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject

class TaskReminderUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val scheduler: ReminderSchedulerContract
) {
    fun observe(taskId: Long): Flow<List<Reminder>> = repository.getRemindersByTaskId(taskId)

    fun observeAllActive(): Flow<List<Reminder>> = repository.getAllActiveReminders()

    suspend fun add(
        taskId: Long,
        titulo: String,
        descripcion: String?,
        fechaRecordatorio: Date,
        tipoRecordatorio: TipoRecordatorio
    ) {
        val reminder = Reminder(
            taskId = taskId,
            titulo = titulo,
            descripcion = descripcion,
            fechaRecordatorio = fechaRecordatorio,
            tipoRecordatorio = tipoRecordatorio
        )
        val id = repository.insertReminder(reminder)
        scheduler.scheduleReminder(reminder.copy(id = id))
    }

    suspend fun update(reminder: Reminder) = repository.updateReminder(reminder)

    suspend fun delete(reminder: Reminder) = repository.deleteReminder(reminder)

    suspend fun setActive(id: Long, activo: Boolean) = repository.updateReminderStatus(id, activo)

    suspend fun setQuick(taskId: Long, taskTitle: String, fecha: Date) {
        repository.getRemindersByTaskIdSync(taskId).forEach {
            scheduler.cancelReminder(it.id)
            repository.deleteReminder(it)
        }
        val reminder = Reminder(
            taskId = taskId,
            titulo = taskTitle,
            fechaRecordatorio = fecha,
            tipoRecordatorio = TipoRecordatorio.UNA_VEZ
        )
        val id = repository.insertReminder(reminder)
        scheduler.scheduleReminder(reminder.copy(id = id))
    }

    suspend fun deleteAllForTask(taskId: Long) {
        repository.getRemindersByTaskIdSync(taskId).forEach {
            scheduler.cancelReminder(it.id)
            repository.deleteReminder(it)
        }
    }
}
