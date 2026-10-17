package com.example.taksy.domain

import com.example.taksy.data.Task
import com.example.taksy.data.TaskRecurrencia
import com.example.taksy.repository.TaskRepository
import com.example.taksy.service.ReminderSchedulerContract
import java.util.Date
import javax.inject.Inject

class CompleteTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val reminderScheduler: ReminderSchedulerContract
) {
    /**
     * Completa la tarea de forma idempotente. Devuelve true solo si esta llamada
     * fue la que la completó; false si ya estaba completada (p. ej. porque la otra
     * ruta de completado — radio button o auto-complete de subtareas — llegó antes).
     *
     * Si la tarea es recurrente, la siguiente ocurrencia hereda sus recordatorios,
     * adelantados con la misma recurrencia que la tarea.
     */
    suspend fun execute(task: Task): Boolean {
        val reminders = repository.getRemindersByTaskIdSync(task.id)
        reminders.forEach { reminderScheduler.cancelReminder(it.id) }
        repository.cancelAllRemindersForTask(task.id)

        if (task.recurrencia == TaskRecurrencia.NINGUNA) {
            return repository.completeTaskIfPending(task.id)
        }

        val now = Date()
        val nextDate = RecurrenceCalculator.advance(task.fechaVencimiento ?: now, task.recurrencia)
        val clone = Task(
            titulo = task.titulo,
            descripcion = task.descripcion,
            fechaVencimiento = nextDate,
            categoriaId = task.categoriaId,
            prioridad = task.prioridad,
            recurrencia = task.recurrencia
        )
        val cloneReminders = reminders.map { reminder ->
            reminder.copy(
                fechaRecordatorio = RecurrenceCalculator.advance(reminder.fechaRecordatorio, task.recurrencia),
                fechaCreacion = now
            )
        }

        val inserted = repository.completeRecurringTask(task, clone, cloneReminders) ?: return false
        // Una fecha pasada (tarea completada con retraso) no se programa: AlarmManager la
        // dispararía al instante. Se guarda igualmente para que la serie no pierda el aviso.
        inserted.filter { it.fechaRecordatorio.after(now) }.forEach { reminderScheduler.scheduleReminder(it) }
        return true
    }
}
