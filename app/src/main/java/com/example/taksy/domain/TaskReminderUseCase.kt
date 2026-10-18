package com.example.taksy.domain

import com.example.taksy.data.Reminder
import com.example.taksy.data.Task
import com.example.taksy.repository.TaskRepository
import com.example.taksy.service.ReminderSchedulerContract
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject

/**
 * Recordatorio de una tarea: uno por tarea y de una sola vez, definido respecto a su fecha de
 * vencimiento (ver [ReminderLead]). Cambiar la fecha mueve el aviso con ella. La repetición vive
 * en la tarea: una tarea recurrente pasa su recordatorio a la siguiente ocurrencia en
 * CompleteTaskUseCase.
 */
class TaskReminderUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val scheduler: ReminderSchedulerContract
) {
    fun observe(taskId: Long): Flow<List<Reminder>> = repository.getRemindersByTaskId(taskId)

    /** Pone el aviso [lead] respecto a la fecha de la tarea; null lo quita. Sin fecha no hay aviso. */
    suspend fun setLead(task: Task, lead: ReminderLead?) {
        val due = task.fechaVencimiento
        if (lead == null || due == null) {
            deleteAllForTask(task.id)
        } else {
            replace(task, lead.reminderFor(due))
        }
    }

    /**
     * Cambia la fecha de la tarea y lleva el aviso con ella, con la misma antelación. Si la
     * antelación no tiene sentido para la nueva fecha (de "con hora" a "sin hora" o al revés)
     * se usa la primera opción del nuevo tipo. Quitar la fecha quita el aviso.
     */
    suspend fun updateDueDate(task: Task, newDue: Date?) {
        repository.updateTask(task.copy(fechaVencimiento = newDue))
        val reminder = repository.getRemindersByTaskIdSync(task.id).firstOrNull { it.activo } ?: return
        val oldDue = task.fechaVencimiento
        when {
            newDue == null -> deleteAllForTask(task.id)
            // Recordatorio antiguo sin fecha de tarea: no hay antelación que conservar
            oldDue == null -> return
            ReminderLead.isAllDay(oldDue) == ReminderLead.isAllDay(newDue) ->
                replace(task, Date(newDue.time + (reminder.fechaRecordatorio.time - oldDue.time)))
            else -> replace(task, ReminderLead.optionsFor(newDue).first().reminderFor(newDue))
        }
    }

    suspend fun deleteAllForTask(taskId: Long) {
        repository.getRemindersByTaskIdSync(taskId).forEach {
            scheduler.cancelReminder(it.id)
            repository.deleteReminder(it)
        }
    }

    private suspend fun replace(task: Task, fecha: Date) {
        deleteAllForTask(task.id)
        val reminder = Reminder(taskId = task.id, titulo = task.titulo, fechaRecordatorio = fecha)
        val id = repository.insertReminder(reminder)
        // Una hora ya pasada se guarda pero no se programa: AlarmManager la dispararía al instante
        if (fecha.after(Date())) scheduler.scheduleReminder(reminder.copy(id = id))
    }
}
