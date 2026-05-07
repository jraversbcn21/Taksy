package com.example.taksy.repository

import com.example.taksy.data.Reminder
import com.example.taksy.data.ReminderDao
import com.example.taksy.data.Subtask
import com.example.taksy.data.SubtaskDao
import com.example.taksy.data.Task
import com.example.taksy.data.TaskDao
import com.example.taksy.data.TaskEstado
import com.example.taksy.data.TipoRecordatorio
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class TaskRepositoryTest {

    // ── Fakes ──────────────────────────────────────────────────────────────

    private class FakeTaskDao : TaskDao {
        val tasks = mutableListOf<Task>()
        var lastUpdated: Task? = null

        override fun getAllTasks(): Flow<List<Task>> = flowOf(tasks)
        override fun getPendingTasks(): Flow<List<Task>> = flowOf(tasks.filter { it.estado == TaskEstado.PENDIENTE })
        override fun getCompletedTasks(): Flow<List<Task>> = flowOf(tasks.filter { it.estado == TaskEstado.COMPLETADA })
        override fun getTasksByCategory(categoryId: Long): Flow<List<Task>> = flowOf(tasks.filter { it.categoriaId == categoryId })
        override fun getTasksWithoutCategory(): Flow<List<Task>> = flowOf(tasks.filter { it.categoriaId == null })
        override fun getReallyPendingTasks(): Flow<List<Task>> = getPendingTasks()
        override fun getReallyCompletedTasks(): Flow<List<Task>> = getCompletedTasks()
        override fun searchTasksByCategory(categoryId: Long, query: String): Flow<List<Task>> =
            flowOf(tasks.filter { it.categoriaId == categoryId && it.titulo.contains(query, ignoreCase = true) })
        override suspend fun getTaskById(id: Long): Task? = tasks.find { it.id == id }
        override suspend fun insertTask(task: Task): Long { tasks.add(task); return task.id }
        override suspend fun updateTask(task: Task) {
            lastUpdated = task
            val idx = tasks.indexOfFirst { it.id == task.id }
            if (idx >= 0) tasks[idx] = task
        }
        override suspend fun deleteTask(task: Task) { tasks.removeIf { it.id == task.id } }
        override suspend fun markTaskAsCompleted(id: Long) {}
        override suspend fun markTaskAsPending(id: Long) {}
        override suspend fun deleteCompletedTasks() { tasks.removeIf { it.estado == TaskEstado.COMPLETADA } }
        override suspend fun getTasksDueToday(startOfDay: Long, endOfDay: Long): List<Task> = emptyList()
        override suspend fun getTasksDueSoon(now: Long, nextWeek: Long): List<Task> = emptyList()
        override suspend fun getOverdueTasks(now: Long): List<Task> = emptyList()
        override suspend fun getAllTasksSync(): List<Task> = tasks.toList()
        override fun getPendingTasksSync(): List<Task> = tasks.filter { it.estado == TaskEstado.PENDIENTE }
        override suspend fun deleteAllTasks() { tasks.clear() }
        override fun searchAllTasks(query: String): Flow<List<Task>> =
            flowOf(tasks.filter { it.titulo.contains(query, ignoreCase = true) })
    }

    private class FakeSubtaskDao : SubtaskDao {
        override fun getSubtasksByTaskId(taskId: Long): Flow<List<Subtask>> = flowOf(emptyList())
        override suspend fun insertSubtask(subtask: Subtask): Long = 1L
        override suspend fun updateSubtask(subtask: Subtask) {}
        override suspend fun deleteSubtask(subtask: Subtask) {}
        override suspend fun deleteSubtasksByTaskId(taskId: Long) {}
        override suspend fun getAllSubtasksSync(): List<Subtask> = emptyList()
        override suspend fun deleteAllSubtasks() {}
    }

    private class FakeReminderDao : ReminderDao {
        override fun getRemindersByTaskId(taskId: Long): Flow<List<Reminder>> = flowOf(emptyList())
        override suspend fun getActiveRemindersDue(now: Long): List<Reminder> = emptyList()
        override fun getAllActiveReminders(): Flow<List<Reminder>> = flowOf(emptyList())
        override suspend fun getAllActiveRemindersSync(): List<Reminder> = emptyList()
        override suspend fun getReminderById(id: Long): Reminder? = null
        override suspend fun insertReminder(reminder: Reminder): Long = 1L
        override suspend fun updateReminder(reminder: Reminder) {}
        override suspend fun deleteReminder(reminder: Reminder) {}
        override suspend fun deleteRemindersByTaskId(taskId: Long) {}
        override suspend fun updateReminderStatus(id: Long, activo: Boolean) {}
        override suspend fun getRemindersByTaskIdSync(taskId: Long): List<Reminder> = emptyList()
        override suspend fun getAllRemindersSync(): List<Reminder> = emptyList()
        override suspend fun deleteAllReminders() {}
    }

    private fun makeRepository(): Pair<FakeTaskDao, TaskRepository> {
        val dao = FakeTaskDao()
        val repo = TaskRepository(dao, FakeSubtaskDao(), FakeReminderDao())
        return dao to repo
    }

    // ── Tests ──────────────────────────────────────────────────────────────

    @Test
    fun `toggleTaskStatus flips PENDIENTE to COMPLETADA`() = runTest {
        val (dao, repo) = makeRepository()
        val task = Task(id = 1L, titulo = "Test", estado = TaskEstado.PENDIENTE)
        dao.tasks.add(task)

        repo.toggleTaskStatus(task)

        assertEquals(TaskEstado.COMPLETADA, dao.lastUpdated?.estado)
    }

    @Test
    fun `toggleTaskStatus flips COMPLETADA to PENDIENTE`() = runTest {
        val (dao, repo) = makeRepository()
        val task = Task(id = 2L, titulo = "Done", estado = TaskEstado.COMPLETADA)
        dao.tasks.add(task)

        repo.toggleTaskStatus(task)

        assertEquals(TaskEstado.PENDIENTE, dao.lastUpdated?.estado)
    }

    @Test
    fun `deleteCompletedTasks removes only completed`() = runTest {
        val (dao, repo) = makeRepository()
        dao.tasks.add(Task(id = 1L, titulo = "Pending", estado = TaskEstado.PENDIENTE))
        dao.tasks.add(Task(id = 2L, titulo = "Done", estado = TaskEstado.COMPLETADA))

        repo.deleteCompletedTasks()

        assertEquals(1, dao.tasks.size)
        assertEquals(TaskEstado.PENDIENTE, dao.tasks[0].estado)
    }

    @Test
    fun `insertTask stores the task`() = runTest {
        val (dao, repo) = makeRepository()
        val task = Task(id = 3L, titulo = "New task")

        repo.insertTask(task)

        assertEquals(1, dao.tasks.size)
        assertEquals("New task", dao.tasks[0].titulo)
    }
}
