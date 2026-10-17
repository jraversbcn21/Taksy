package com.example.taksy.repository

import com.example.taksy.data.AppDatabase
import com.example.taksy.data.Reminder
import com.example.taksy.data.ReminderDao
import com.example.taksy.data.Subtask
import com.example.taksy.data.SubtaskDao
import com.example.taksy.data.Task
import com.example.taksy.data.TaskDao
import com.example.taksy.data.TaskEstado
import com.example.taksy.data.TipoRecordatorio
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
        override suspend fun deleteCompletedTasks() { tasks.removeIf { it.estado == TaskEstado.COMPLETADA } }
        override suspend fun getTasksDueToday(startOfDay: Long, endOfDay: Long): List<Task> = emptyList()
        override suspend fun getTasksDueSoon(now: Long, nextWeek: Long): List<Task> = emptyList()
        override suspend fun getOverdueTasks(now: Long): List<Task> = emptyList()
        override suspend fun getAllTasksSync(): List<Task> = tasks.toList()
        override fun getPendingTasksSync(): List<Task> = tasks.filter { it.estado == TaskEstado.PENDIENTE }
        override suspend fun deleteAllTasks() { tasks.clear() }
        override fun searchAllTasks(query: String): Flow<List<Task>> =
            flowOf(tasks.filter { it.titulo.contains(query, ignoreCase = true) })
        override fun getArchivedTasksByCategory(categoryId: Long): Flow<List<Task>> =
            flowOf(tasks.filter { it.categoriaId == categoryId && it.archivada })
        override suspend fun archiveTask(taskId: Long) {
            val idx = tasks.indexOfFirst { it.id == taskId }
            if (idx >= 0) tasks[idx] = tasks[idx].copy(archivada = true)
        }
        override suspend fun unarchiveTask(taskId: Long) {
            val idx = tasks.indexOfFirst { it.id == taskId }
            if (idx >= 0) tasks[idx] = tasks[idx].copy(archivada = false)
        }
        override suspend fun updateTasks(tasks: List<Task>) {
            tasks.forEach { updateTask(it) }
        }
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
        val reminders = mutableListOf<Reminder>()

        override fun getRemindersByTaskId(taskId: Long): Flow<List<Reminder>> = flowOf(reminders.filter { it.taskId == taskId })
        override suspend fun getActiveRemindersDue(now: Long): List<Reminder> = emptyList()
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
        override suspend fun advanceReminderIfUnchanged(id: Long, expectedDate: Date, nextDate: Date): Int {
            val idx = reminders.indexOfFirst { it.id == id && it.fechaRecordatorio == expectedDate && it.activo }
            if (idx < 0) return 0
            reminders[idx] = reminders[idx].copy(fechaRecordatorio = nextDate)
            return 1
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
    private fun makeTransactionalRepository(): Triple<FakeTaskDao, FakeReminderDao, TaskRepository> {
        mockkStatic("androidx.room.RoomDatabaseKt")
        val db = mockk<AppDatabase>(relaxed = true)
        val blockSlot = slot<suspend () -> Reminder?>()
        coEvery { db.withTransaction(capture(blockSlot)) } coAnswers { blockSlot.captured.invoke() }
        val taskDao = FakeTaskDao()
        val reminderDao = FakeReminderDao()
        return Triple(taskDao, reminderDao, TaskRepository(db, taskDao, FakeSubtaskDao(), reminderDao))
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.room.RoomDatabaseKt")
    }

    private fun recurringReminder(fecha: Date = Date(1_000_000L)) = Reminder(
        id = 10L,
        taskId = 1L,
        titulo = "Recordatorio",
        fechaRecordatorio = fecha,
        tipoRecordatorio = TipoRecordatorio.DIARIO
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
    fun `deleteTask removes specific task`() = runTest {
        val (dao, repo) = makeRepository()
        val task1 = Task(id = 1L, titulo = "Keep")
        val task2 = Task(id = 2L, titulo = "Delete")
        dao.tasks.addAll(listOf(task1, task2))

        repo.deleteTask(task2)

        assertEquals(1, dao.tasks.size)
        assertEquals("Keep", dao.tasks[0].titulo)
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
    fun `getTasksByFilter TODAS returns all tasks`() = runTest {
        val (dao, repo) = makeRepository()
        dao.tasks.add(Task(id = 1L, titulo = "A", estado = TaskEstado.PENDIENTE))
        dao.tasks.add(Task(id = 2L, titulo = "B", estado = TaskEstado.COMPLETADA))

        val result = repo.getTasksByFilter(TaskFilter.TODAS).first()

        assertEquals(2, result.size)
    }

    @Test
    fun `getTasksByFilter PENDIENTES returns only pending`() = runTest {
        val (dao, repo) = makeRepository()
        dao.tasks.add(Task(id = 1L, titulo = "A", estado = TaskEstado.PENDIENTE))
        dao.tasks.add(Task(id = 2L, titulo = "B", estado = TaskEstado.COMPLETADA))

        val result = repo.getTasksByFilter(TaskFilter.PENDIENTES).first()

        assertEquals(1, result.size)
        assertEquals(TaskEstado.PENDIENTE, result[0].estado)
    }

    @Test
    fun `getTasksByFilter COMPLETADAS returns only completed`() = runTest {
        val (dao, repo) = makeRepository()
        dao.tasks.add(Task(id = 1L, titulo = "A", estado = TaskEstado.PENDIENTE))
        dao.tasks.add(Task(id = 2L, titulo = "B", estado = TaskEstado.COMPLETADA))

        val result = repo.getTasksByFilter(TaskFilter.COMPLETADAS).first()

        assertEquals(1, result.size)
        assertEquals(TaskEstado.COMPLETADA, result[0].estado)
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
        reminderDao.reminders.add(recurringReminder().copy(id = 1L, taskId = 1L))
        reminderDao.reminders.add(recurringReminder().copy(id = 2L, taskId = 1L))
        reminderDao.reminders.add(recurringReminder().copy(id = 3L, taskId = 2L))

        repo.cancelAllRemindersForTask(1L)

        assertEquals(listOf(false, false, true), reminderDao.reminders.map { it.activo })
    }

    @Test
    fun `advanceRecurringReminder advances date and returns updated reminder`() = runTest {
        val (taskDao, reminderDao, repo) = makeTransactionalRepository()
        taskDao.tasks.add(Task(id = 1L, titulo = "Test"))
        val reminder = recurringReminder()
        reminderDao.reminders.add(reminder)
        val next = Date(2_000_000L)

        val result = repo.advanceRecurringReminder(reminder, next)

        assertEquals(next, result?.fechaRecordatorio)
        assertEquals(next, reminderDao.reminders.single().fechaRecordatorio)
    }

    @Test
    fun `advanceRecurringReminder with duplicate fire advances only once`() = runTest {
        val (taskDao, reminderDao, repo) = makeTransactionalRepository()
        taskDao.tasks.add(Task(id = 1L, titulo = "Test"))
        val snapshot = recurringReminder()
        reminderDao.reminders.add(snapshot)
        val next = Date(2_000_000L)

        // Dos disparos de alarma que leyeron el mismo snapshot antes de escribir
        val first = repo.advanceRecurringReminder(snapshot, next)
        val second = repo.advanceRecurringReminder(snapshot, next)

        assertNotNull(first)
        assertNull(second)
        assertEquals(next, reminderDao.reminders.single().fechaRecordatorio)
    }

    @Test
    fun `advanceRecurringReminder does not reactivate a reminder deactivated after read`() = runTest {
        val (taskDao, reminderDao, repo) = makeTransactionalRepository()
        taskDao.tasks.add(Task(id = 1L, titulo = "Test"))
        val snapshot = recurringReminder()
        reminderDao.reminders.add(snapshot)

        // Se cancela tras la lectura del receiver pero antes de su escritura
        repo.cancelAllRemindersForTask(1L)
        val result = repo.advanceRecurringReminder(snapshot, Date(2_000_000L))

        assertNull(result)
        val stored = reminderDao.reminders.single()
        assertFalse(stored.activo)
        assertEquals(snapshot.fechaRecordatorio, stored.fechaRecordatorio)
    }

    @Test
    fun `advanceRecurringReminder skips when task is completed`() = runTest {
        val (taskDao, reminderDao, repo) = makeTransactionalRepository()
        taskDao.tasks.add(Task(id = 1L, titulo = "Test", estado = TaskEstado.COMPLETADA))
        val reminder = recurringReminder()
        reminderDao.reminders.add(reminder)

        val result = repo.advanceRecurringReminder(reminder, Date(2_000_000L))

        assertNull(result)
        assertEquals(reminder.fechaRecordatorio, reminderDao.reminders.single().fechaRecordatorio)
    }

    @Test
    fun `advanceRecurringReminder skips when task no longer exists`() = runTest {
        val (_, reminderDao, repo) = makeTransactionalRepository()
        val reminder = recurringReminder()
        reminderDao.reminders.add(reminder)

        assertNull(repo.advanceRecurringReminder(reminder, Date(2_000_000L)))
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
