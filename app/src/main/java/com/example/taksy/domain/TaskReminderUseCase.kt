package com.example.taksy.domain

import com.example.taksy.data.Reminder
import com.example.taksy.data.TipoRecordatorio
import com.example.taksy.repository.TaskRepository
import com.example.taksy.service.ReminderSchedulerContract
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject

/**
 * Recordatorios de una tarea. Desde la UI solo existe el recordatorio rápido: uno por
 * tarea y de una sola vez. Los recordatorios con repetición propia (DIARIO/SEMANAL/MENSUAL)
 * ya no se pueden crear; ReminderReceiver los sigue atendiendo por compatibilidad con
 * datos antiguos o importados. Una tarea recurrente pasa su recordatorio a la siguiente
 * ocurrencia en CompleteTaskUseCase.
 */
class TaskReminderUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val scheduler: ReminderSchedulerContract
) {
    fun observe(taskId: Long): Flow<List<Reminder>> = repository.getRemindersByTaskId(taskId)

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
