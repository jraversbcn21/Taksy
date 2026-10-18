package com.example.taksy.repository

import com.example.taksy.data.AppDatabase
import com.example.taksy.data.Reminder
import com.example.taksy.data.ReminderDao
import com.example.taksy.data.Subtask
import com.example.taksy.data.SubtaskDao
import com.example.taksy.data.Task
import com.example.taksy.data.TaskDao
import com.example.taksy.data.TaskEstado
import androidx.room.withTransaction
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class TaskRepositoryTest {

    // ── Fakes ──────────────────────────────────────────────────────────────

    private class FakeTaskDao : TaskDao {
        val tasks = mutableListOf<Task>()
        var lastUpdated: Task? = null

        override fun getAllTasks(): Flow<List<Task>> = flowOf(tasks)
        override fun getTasksByCategory(categoryId: Long): Flow<List<Task>> = flowOf(tasks.filter { it.categoriaId == categoryId })
        override fun getReallyPendingTasks(): Flow<List<Task>> = flowOf(tasks.filter { it.estado == TaskEstado.PENDIENTE })
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
        override suspend fun completeTaskIfPending(id: Long, fechaCompletada: Date): Int {
            val idx = tasks.indexOfFirst { it.id == id && it.estado == TaskEstado.PENDIENTE }
            if (idx < 0) return 0
            tasks[idx] = tasks[idx].copy(estado = TaskEstado.COMPLETADA, fechaCompletada = fechaCompletada)
            lastUpdated = tasks[idx]
            return 1
        }
        override suspend fun markTaskAsPending(id: Long) {
            val idx = tasks.indexOfFirst { it.id == id }
            if (idx >= 0) {
                tasks[idx] = tasks[idx].copy(estado = TaskEstado.PENDIENTE, fechaCompletada = null)
                lastUpdated = tasks[idx]
            }
        }
        override suspend fun getAllTasksByCategorySync(categoryId: Long): List<Task> = tasks.filter { it.categoriaId == categoryId }
        override suspend fun getAllTasksSync(): List<Task> = tasks.toList()
        override fun getPendingTasksSync(): List<Task> = tasks.filter { it.estado == TaskEstado.PENDIENTE }
        override suspend fun deleteAllTasks() { tasks.clear() }
        override fun searchAllTasks(query: String): Flow<List<Task>> =
            flowOf(tasks.filter { it.titulo.contains(query, ignoreCase = true) })
    }

    private class FakeSubtaskDao : SubtaskDao {
        val subtasks = mutableListOf<Subtask>()

        override fun getSubtasksByTaskId(taskId: Long): Flow<List<Subtask>> = flowOf(subtasks.filter { it.taskId == taskId })
        override suspend fun insertSubtask(subtask: Subtask): Long { subtasks.add(subtask); return subtask.id }
        override suspend fun updateSubtask(subtask: Subtask) {}
        override suspend fun deleteSubtask(subtask: Subtask) { subtasks.removeIf { it.id == subtask.id } }
        override suspend fun getSubtasksByTaskIdSync(taskId: Long): List<Subtask> = subtasks.filter { it.taskId == taskId }
        override suspend fun deleteSubtasksByTaskId(taskId: Long) { subtasks.removeIf { it.taskId == taskId } }
        override suspend fun getAllSubtasksSync(): List<Subtask> = subtasks.toList()
        override suspend fun deleteAllSubtasks() { subtasks.clear() }
    }

    private class FakeReminderDao : ReminderDao {
        val reminders = mutableListOf<Reminder>()

        override fun getRemindersByTaskId(taskId: Long): Flow<List<Reminder>> = flowOf(reminders.filter { it.taskId == taskId })
        override fun getAllActiveReminders(): Flow<List<Reminder>> = flowOf(reminders.filter { it.activo })
        override suspend fun getAllActiveRemindersSync(): List<Reminder> = reminders.filter { it.activo }
        override suspend fun getReminderById(id: Long): Reminder? = reminders.find { it.id == id }
        override suspend fun insertReminder(reminder: Reminder): Long { reminders.add(reminder); return reminder.id }
        override suspend fun updateReminder(reminder: Reminder) {
            val idx = reminders.indexOfFirst { it.id == reminder.id }
            if (idx >= 0) reminders[idx] = reminder
        }
        override suspend fun deleteReminder(reminder: Reminder) { reminders.removeIf { it.id == reminder.id } }
        override suspend fun deleteRemindersByTaskId(taskId: Long) { reminders.removeIf { it.taskId == taskId } }
        override suspend fun updateReminderStatus(id: Long, activo: Boolean) {
            val idx = reminders.indexOfFirst { it.id == id }
            if (idx >= 0) reminders[idx] = reminders[idx].copy(activo = activo)
        }
        override suspend fun deactivateRemindersByTaskId(taskId: Long) {
            reminders.replaceAll { if (it.taskId == taskId) it.copy(activo = false) else it }
        }
        override suspend fun getRemindersByTaskIdSync(taskId: Long): List<Reminder> = reminders.filter { it.taskId == taskId }
        override suspend fun getAllRemindersSync(): List<Reminder> = reminders.toList()
        override suspend fun deleteAllReminders() { reminders.clear() }
    }

    private fun makeRepository(): Pair<FakeTaskDao, TaskRepository> {
        val dao = FakeTaskDao()
        val repo = TaskRepository(mockk(relaxed = true), dao, FakeSubtaskDao(), FakeReminderDao())
        return dao to repo
    }

    /** Repositorio con withTransaction mockeado para ejecutar el bloque directamente sobre los fakes. */
    private fun makeTransactionalRepository(
        subtaskDao: FakeSubtaskDao = FakeSubtaskDao()
    ): Triple<FakeTaskDao, FakeReminderDao, TaskRepository> {
        mockkStatic("androidx.room.RoomDatabaseKt")
        val db = mockk<AppDatabase>(relaxed = true)
        val blockSlot = slot<suspend () -> Any?>()
        coEvery { db.withTransaction(capture(blockSlot)) } coAnswers { blockSlot.captured.invoke() }
        val taskDao = FakeTaskDao()
        val reminderDao = FakeReminderDao()
        return Triple(taskDao, reminderDao, TaskRepository(db, taskDao, subtaskDao, reminderDao))
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.room.RoomDatabaseKt")
    }

    private fun reminder(fecha: Date = Date(1_000_000L)) = Reminder(
        id = 10L,
        taskId = 1L,
        titulo = "Recordatorio",
        fechaRecordatorio = fecha
    )

    // ── Tests ──────────────────────────────────────────────────────────────

    @Test
    fun `completeTaskIfPending completes a pending task and returns true`() = runTest {
        val (dao, repo) = makeRepository()
        dao.tasks.add(Task(id = 1L, titulo = "Test", estado = TaskEstado.PENDIENTE))

        val won = repo.completeTaskIfPending(1L)

        assertEquals(true, won)
        assertEquals(TaskEstado.COMPLETADA, dao.tasks[0].estado)
    }

    @Test
    fun `completeTaskIfPending returns false when task already completed`() = runTest {
        val (dao, repo) = makeRepository()
        dao.tasks.add(Task(id = 1L, titulo = "Test", estado = TaskEstado.PENDIENTE))

        val first = repo.completeTaskIfPending(1L)
        val second = repo.completeTaskIfPending(1L)

        assertEquals(true, first)
        assertEquals(false, second)
    }

    @Test
    fun `markTaskPending reverts completion and clears fechaCompletada`() = runTest {
        val (dao, repo) = makeRepository()
        dao.tasks.add(Task(id = 2L, titulo = "Done", estado = TaskEstado.COMPLETADA, fechaCompletada = Date()))

        repo.markTaskPending(2L)

        assertEquals(TaskEstado.PENDIENTE, dao.tasks[0].estado)
        assertEquals(null, dao.tasks[0].fechaCompletada)
    }

    @Test
    fun `deleteTasksInCategory removes only that category's tasks and returns their reminder ids`() = runTest {
        val (taskDao, reminderDao, repo) = makeTransactionalRepository()
        taskDao.tasks.add(Task(id = 1L, titulo = "A", categoriaId = 5L))
        taskDao.tasks.add(Task(id = 2L, titulo = "B", categoriaId = 5L, estado = TaskEstado.COMPLETADA))
        taskDao.tasks.add(Task(id = 3L, titulo = "C", categoriaId = 6L))
        reminderDao.reminders.add(reminder().copy(id = 11L, taskId = 1L))
        reminderDao.reminders.add(reminder().copy(id = 12L, taskId = 2L))
        reminderDao.reminders.add(reminder().copy(id = 13L, taskId = 3L))

        val reminderIds = repo.deleteTasksInCategory(5L)

        assertEquals(setOf(11L, 12L), reminderIds.toSet())
        assertEquals(listOf(3L), taskDao.tasks.map { it.id })
        assertEquals(listOf(13L), reminderDao.reminders.map { it.id })
    }

    @Test
    fun `insertTask stores the task`() = runTest {
        val (dao, repo) = makeRepository()
        val task = Task(id = 3L, titulo = "New task")

        repo.insertTask(task)

        assertEquals(1, dao.tasks.size)
        assertEquals("New task", dao.tasks[0].titulo)
    }

    @Test
    fun `getTaskById returns correct task`() = runTest {
        val (dao, repo) = makeRepository()
        dao.tasks.add(Task(id = 5L, titulo = "Find me"))
        dao.tasks.add(Task(id = 6L, titulo = "Not me"))

        val result = repo.getTaskById(5L)

        assertEquals("Find me", result?.titulo)
    }

    @Test
    fun `getTaskById returns null for missing id`() = runTest {
        val (_, repo) = makeRepository()
        val result = repo.getTaskById(999L)
        assertEquals(null, result)
    }

    @Test
    fun `deleteTaskWithChildren removes the task with its subtasks and reminders`() = runTest {
        val subtaskDao = FakeSubtaskDao()
        val (taskDao, reminderDao, repo) = makeTransactionalRepository(subtaskDao)
        val keep = Task(id = 1L, titulo = "Keep")
        val delete = Task(id = 2L, titulo = "Delete")
        taskDao.tasks.addAll(listOf(keep, delete))
        subtaskDao.subtasks.addAll(listOf(
            Subtask(id = 10L, taskId = 1L, titulo = "keep"),
            Subtask(id = 11L, taskId = 2L, titulo = "a"),
            Subtask(id = 12L, taskId = 2L, titulo = "b")
        ))
        reminderDao.reminders.addAll(listOf(reminder().copy(id = 20L, taskId = 1L), reminder().copy(id = 21L, taskId = 2L)))

        val deleted = repo.deleteTaskWithChildren(delete)

        assertEquals(listOf("Keep"), taskDao.tasks.map { it.titulo })
        assertEquals(listOf(10L), subtaskDao.subtasks.map { it.id })
        assertEquals(listOf(20L), reminderDao.reminders.map { it.id })
        assertEquals(listOf(11L, 12L), deleted.subtasks.map { it.id })
        assertEquals(listOf(21L), deleted.reminders.map { it.id })
    }

    @Test
    fun `restoreDeletedTask brings back the task, subtasks and reminders with their ids`() = runTest {
        val subtaskDao = FakeSubtaskDao()
        val (taskDao, reminderDao, repo) = makeTransactionalRepository(subtaskDao)
        val task = Task(id = 2L, titulo = "Delete")
        taskDao.tasks.add(task)
        subtaskDao.subtasks.add(Subtask(id = 11L, taskId = 2L, titulo = "a"))
        reminderDao.reminders.add(reminder().copy(id = 21L, taskId = 2L))

        repo.restoreDeletedTask(repo.deleteTaskWithChildren(task))

        assertEquals(listOf(2L), taskDao.tasks.map { it.id })
        assertEquals(listOf(11L), subtaskDao.subtasks.map { it.id })
        assertEquals(listOf(21L), reminderDao.reminders.map { it.id })
    }

    @Test
    fun `updateTask modifies existing task`() = runTest {
        val (dao, repo) = makeRepository()
        val task = Task(id = 1L, titulo = "Original")
        dao.tasks.add(task)

        repo.updateTask(task.copy(titulo = "Updated"))

        assertEquals("Updated", dao.tasks[0].titulo)
    }

    @Test
    fun `getReallyPendingTasks returns only pending`() = runTest {
        val (dao, repo) = makeRepository()
        dao.tasks.add(Task(id = 1L, titulo = "A", estado = TaskEstado.PENDIENTE))
        dao.tasks.add(Task(id = 2L, titulo = "B", estado = TaskEstado.COMPLETADA))

        val result = repo.getReallyPendingTasks().first()

        assertEquals(1, result.size)
        assertEquals(TaskEstado.PENDIENTE, result[0].estado)
    }

    @Test
    fun `searchAllTasks filters by query`() = runTest {
        val (dao, repo) = makeRepository()
        dao.tasks.add(Task(id = 1L, titulo = "Buy groceries"))
        dao.tasks.add(Task(id = 2L, titulo = "Clean kitchen"))

        val result = repo.searchAllTasks("buy").first()

        assertEquals(1, result.size)
        assertEquals("Buy groceries", result[0].titulo)
    }

    @Test
    fun `searchAllTasks is case insensitive`() = runTest {
        val (dao, repo) = makeRepository()
        dao.tasks.add(Task(id = 1L, titulo = "Buy GROCERIES"))

        val result = repo.searchAllTasks("groceries").first()

        assertEquals(1, result.size)
    }

    @Test
    fun `cancelAllRemindersForTask deactivates only reminders of that task`() = runTest {
        val (_, reminderDao, repo) = makeTransactionalRepository()
        reminderDao.reminders.add(reminder().copy(id = 1L, taskId = 1L))
        reminderDao.reminders.add(reminder().copy(id = 2L, taskId = 1L))
        reminderDao.reminders.add(reminder().copy(id = 3L, taskId = 2L))

        repo.cancelAllRemindersForTask(1L)

        assertEquals(listOf(false, false, true), reminderDao.reminders.map { it.activo })
    }

    @Test
    fun `searchTasksByCategory filters by category and query`() = runTest {
        val (dao, repo) = makeRepository()
        dao.tasks.add(Task(id = 1L, titulo = "Buy milk", categoriaId = 1L))
        dao.tasks.add(Task(id = 2L, titulo = "Buy bread", categoriaId = 2L))
        dao.tasks.add(Task(id = 3L, titulo = "Clean", categoriaId = 1L))

        val result = repo.searchTasksByCategory(1L, "buy").first()

        assertEquals(1, result.size)
        assertEquals("Buy milk", result[0].titulo)
    }
}
