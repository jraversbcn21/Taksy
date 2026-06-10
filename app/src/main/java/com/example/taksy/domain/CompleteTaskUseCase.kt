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
    suspend fun execute(task: Task) {
        repository.getRemindersByTaskIdSync(task.id).forEach { reminderScheduler.cancelReminder(it.id) }
        repository.cancelAllRemindersForTask(task.id)

        if (task.recurrencia != TaskRecurrencia.NINGUNA) {
            val nextDate = RecurrenceCalculator.advance(task.fechaVencimiento ?: Date(), task.recurrencia)
            val clone = Task(
                titulo = task.titulo,
                descripcion = task.descripcion,
                fechaVencimiento = nextDate,
                categoriaId = task.categoriaId,
                prioridad = task.prioridad,
                recurrencia = task.recurrencia
            )
            repository.completeRecurringTask(task, clone)
        } else {
            repository.toggleTaskStatus(task)
        }
    }
}
