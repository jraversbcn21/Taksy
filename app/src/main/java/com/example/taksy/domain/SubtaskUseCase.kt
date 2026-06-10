package com.example.taksy.domain

import com.example.taksy.data.Subtask
import com.example.taksy.data.TaskEstado
import com.example.taksy.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SubtaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val completeTaskUseCase: CompleteTaskUseCase
) {
    fun observe(taskId: Long): Flow<List<Subtask>> = repository.getSubtasksByTaskId(taskId)

    suspend fun add(taskId: Long, titulo: String) {
        if (titulo.isBlank()) return
        repository.insertSubtask(Subtask(taskId = taskId, titulo = titulo.trim()))
    }

    /** Returns true when toggling caused the parent task to auto-complete. */
    suspend fun toggle(subtask: Subtask): Boolean {
        val updated = subtask.copy(
            estado = if (subtask.estado == TaskEstado.PENDIENTE) TaskEstado.COMPLETADA else TaskEstado.PENDIENTE
        )
        repository.updateSubtask(updated)

        val siblings = repository.getSubtasksByTaskId(subtask.taskId).first()
        if (siblings.isNotEmpty() && siblings.all { it.estado == TaskEstado.COMPLETADA }) {
            val task = repository.getTaskById(subtask.taskId)
            if (task != null && task.estado == TaskEstado.PENDIENTE) {
                completeTaskUseCase.execute(task)
                return true
            }
        }
        return false
    }

    suspend fun delete(subtask: Subtask) = repository.deleteSubtask(subtask)
}
