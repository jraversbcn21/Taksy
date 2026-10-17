package com.example.taksy.domain

import androidx.room.withTransaction
import com.example.taksy.data.AppDatabase
import com.example.taksy.data.Category
import com.example.taksy.repository.CategoryRepository
import com.example.taksy.repository.TaskRepository
import com.example.taksy.service.ReminderSchedulerContract
import javax.inject.Inject

/**
 * Elimina una categoría con todas sus tareas. No hay clave foránea entre tareas y
 * categorías, así que borrar solo la categoría dejaría tareas huérfanas que no se ven
 * en ninguna pantalla pero siguen en el widget y con alarmas programadas.
 */
class DeleteCategoryUseCase @Inject constructor(
    private val appDatabase: AppDatabase,
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository,
    private val reminderScheduler: ReminderSchedulerContract
) {
    suspend fun execute(category: Category) {
        val reminderIds = appDatabase.withTransaction {
            taskRepository.deleteTasksInCategory(category.id)
                .also { categoryRepository.deleteCategory(category) }
        }
        // Las alarmas se cancelan tras el commit: si la transacción falla, siguen programadas
        reminderIds.forEach { reminderScheduler.cancelReminder(it) }
    }
}
