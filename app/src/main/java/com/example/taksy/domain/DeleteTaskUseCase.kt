package com.example.taksy.domain

import com.example.taksy.data.Task
import com.example.taksy.repository.DeletedTask
import com.example.taksy.repository.TaskRepository
import com.example.taksy.service.ReminderSchedulerContract
import javax.inject.Inject

/**
 * Borra una tarea con sus subtareas y recordatorios, y permite deshacerlo.
 * No hay clave foránea entre esas tablas: borrar solo la fila de la tarea dejaba subtareas
 * y recordatorios huérfanos, con alarmas que seguían programadas.
 */
class DeleteTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val scheduler: ReminderSchedulerContract
) {
    /** Devuelve lo borrado, que es lo que necesita [restore] para deshacer. */
    suspend fun delete(task: Task): DeletedTask {
        val deleted = repository.deleteTaskWithChildren(task)
        // Las alarmas se cancelan tras el commit: si la transacción falla, siguen programadas
        deleted.reminders.forEach { scheduler.cancelReminder(it.id) }
        return deleted
    }

    suspend fun restore(deleted: DeletedTask) {
        repository.restoreDeletedTask(deleted)
        // Una fecha ya pasada no se programa: AlarmManager la dispararía al instante
        val now = System.currentTimeMillis()
        deleted.reminders
            .filter { it.activo && it.fechaRecordatorio.time > now }
            .forEach { scheduler.scheduleReminder(it) }
    }
}
