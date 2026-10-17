package com.example.taksy.domain

import androidx.room.withTransaction
import com.example.taksy.data.AppDatabase
import com.example.taksy.data.Reminder
import com.example.taksy.data.ReminderDao
import com.example.taksy.data.Subtask
import com.example.taksy.data.SubtaskDao
import com.example.taksy.data.Task
import com.example.taksy.data.TaskDao
import com.example.taksy.data.TaskEstado
import com.example.taksy.data.TaskRecurrencia
import com.example.taksy.repository.TaskRepository
import com.example.taksy.service.ReminderSchedulerContract
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

/**
 * Regression tests del "recurring task duplication bug": dos rutas de completado
 * (radio button y auto-complete de subtareas) ejecutando CompleteTaskUseCase en
 * ventanas solapadas no deben producir dos clones de la siguiente ocurrencia.
 */
class CompleteTaskUseCaseTest {

    // ── Fakes ──────────────────────────────────────────────────────────────

    private class FakeTaskDao : TaskDao {
        val tasks = mutableListOf<Task>()
        private var nextId = 100L

        override fun getAllTasks(): Flow<List<Task>> = flowOf(tasks.toList())
        override fun getPendingTasks(): Flow<List<Task>> = flowOf(tasks.filter { it.estado == TaskEstado.PENDIENTE })
        override fun getCompletedTasks(): Flow<List<Task>> = flowOf(tasks.filter { it.estado == TaskEstado.COMPLETADA })
        override fun getTasksByCategory(categoryId: Long): Flow<List<Task>> = flowOf(tasks.filter { it.categoriaId == categoryId })
        override fun getTasksWithoutCategory(): Flow<List<Task>> = flowOf(tasks.filter { it.categoriaId == null })
        override fun getReallyPendingTasks(): Flow<List<Task>> = getPendingTasks()
        override fun getReallyCompletedTasks(): Flow<List<Task>> = getCompletedTasks()
        override fun searchTasksByCategory(categoryId: Long, query: String): Flow<List<Task>> = flowOf(emptyList())
        override fun searchAllTasks(query: String): Flow<List<Task>> = flowOf(emptyList())
        override suspend fun getTaskById(id: Long): Task? = tasks.find { it.id == id }
        override suspend fun insertTask(task: Task): Long {
            val id = if (task.id == 0L) nextId++ else task.id
            tasks.add(task.copy(id = id))
            return id
        }
        override suspend fun updateTask(task: Task) {
            val idx = tasks.indexOfFirst { it.id == task.id }
            if (idx >= 0) tasks[idx] = task
        }
        override suspend fun deleteTask(task: Task) { tasks.removeIf { it.id == task.id } }
        override suspend fun completeTaskIfPending(id: Long, fechaCompletada: Date): Int {
            val idx = tasks.indexOfFirst { it.id == id && it.estado == TaskEstado.PENDIENTE }
            if (idx < 0) return 0
            tasks[idx] = tasks[idx].copy(estado = TaskEstado.COMPLETADA, fechaCompletada = fechaCompletada)
            return 1
        }
        override suspend fun markTaskAsPending(id: Long) {
            val idx = tasks.indexOfFirst { it.id == id }
            if (idx >= 0) tasks[idx] = tasks[idx].copy(estado = TaskEstado.PENDIENTE, fechaCompletada = null)
        }
        override suspend fun getAllTasksByCategorySync(categoryId: Long): List<Task> = tasks.filter { it.categoriaId == categoryId }
        override suspend fun getTasksDueToday(startOfDay: Long, endOfDay: Long): List<Task> = emptyList()
        override suspend fun getTasksDueSoon(now: Long, nextWeek: Long): List<Task> = emptyList()
        override suspend fun getOverdueTasks(now: Long): List<Task> = emptyList()
        override suspend fun getAllTasksSync(): List<Task> = tasks.toList()
        override fun getPendingTasksSync(): List<Task> = tasks.filter { it.estado == TaskEstado.PENDIENTE }
        override suspend fun deleteAllTasks() { tasks.clear() }
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
        private var nextReminderId = 100L
        override suspend fun insertReminder(reminder: Reminder): Long {
            val id = if (reminder.id == 0L) nextReminderId++ else reminder.id
            reminders.add(reminder.copy(id = id))
            return id
        }
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

    private class FakeScheduler : ReminderSchedulerContract {
        val cancelledIds = mutableListOf<Long>()
        val scheduled = mutableListOf<Reminder>()
        override fun scheduleReminder(reminder: Reminder) { scheduled.add(reminder) }
        override fun cancelReminder(reminderId: Long) { cancelledIds.add(reminderId) }
    }

    // ── Setup ──────────────────────────────────────────────────────────────

    private lateinit var taskDao: FakeTaskDao
    private lateinit var reminderDao: FakeReminderDao
    private lateinit var scheduler: FakeScheduler
    private lateinit var useCase: CompleteTaskUseCase

    @Before
    fun setup() {
        taskDao = FakeTaskDao()
        reminderDao = FakeReminderDao()
        scheduler = FakeScheduler()

        // withTransaction es una extensión de room-ktx: se mockea estáticamente
        // para que ejecute el bloque directamente sobre los fakes.
        mockkStatic("androidx.room.RoomDatabaseKt")
        val db = mockk<AppDatabase>(relaxed = true)
        val blockSlot = slot<suspend () -> Any?>()
        coEvery { db.withTransaction(capture(blockSlot)) } coAnswers { blockSlot.captured.invoke() }

        val repository = TaskRepository(db, taskDao, FakeSubtaskDao(), reminderDao)
        useCase = CompleteTaskUseCase(repository, scheduler)
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.room.RoomDatabaseKt")
    }

    private fun recurringTask(id: Long = 1L) = Task(
        id = id,
        titulo = "Recurrente",
        fechaVencimiento = Date(),
        recurrencia = TaskRecurrencia.DIARIA,
        estado = TaskEstado.PENDIENTE
    )

    // ── Tests ──────────────────────────────────────────────────────────────

    @Test
    fun `execute completes recurring task and inserts exactly one clone`() = runTest {
        taskDao.tasks.add(recurringTask())

        val won = useCase.execute(taskDao.tasks[0])

        assertTrue(won)
        assertEquals(2, taskDao.tasks.size)
        assertEquals(TaskEstado.COMPLETADA, taskDao.tasks.first { it.id == 1L }.estado)
        assertEquals(1, taskDao.tasks.count { it.estado == TaskEstado.PENDIENTE })
    }

    @Test
    fun `second execute with stale snapshot does not insert a second clone`() = runTest {
        // Reproduce la carrera: radio button y auto-complete de subtareas capturan
        // el mismo snapshot PENDIENTE de la tarea y ambos llaman a execute().
        taskDao.tasks.add(recurringTask())
        val staleSnapshot = taskDao.tasks[0]

        val first = useCase.execute(staleSnapshot)
        val second = useCase.execute(staleSnapshot)

        assertTrue(first)
        assertFalse(second)
        assertEquals(2, taskDao.tasks.size)
        assertEquals(1, taskDao.tasks.count { it.estado == TaskEstado.PENDIENTE })
    }

    @Test
    fun `execute on non-recurring task is idempotent`() = runTest {
        taskDao.tasks.add(Task(id = 1L, titulo = "Simple", estado = TaskEstado.PENDIENTE))
        val staleSnapshot = taskDao.tasks[0]

        val first = useCase.execute(staleSnapshot)
        val second = useCase.execute(staleSnapshot)

        assertTrue(first)
        assertFalse(second)
        assertEquals(1, taskDao.tasks.size)
        assertEquals(TaskEstado.COMPLETADA, taskDao.tasks[0].estado)
    }

    @Test
    fun `execute cancels scheduler and deactivates reminders even when it loses the race`() = runTest {
        taskDao.tasks.add(recurringTask())
        reminderDao.reminders.add(
            Reminder(id = 7L, taskId = 1L, titulo = "R", fechaRecordatorio = Date(), fechaCreacion = Date())
        )
        val staleSnapshot = taskDao.tasks[0]

        useCase.execute(staleSnapshot)
        useCase.execute(staleSnapshot)

        assertTrue(scheduler.cancelledIds.contains(7L))
        assertFalse(reminderDao.reminders.single { it.id == 7L }.activo)
        // El segundo execute perdió la carrera: no hereda un segundo recordatorio
        assertEquals(1, reminderDao.reminders.count { it.taskId != 1L })
    }

    // ── Herencia de recordatorios ─────────────────────────────────────────

    @Test
    fun `clone inherits the reminder advanced by the task recurrence and it is scheduled`() = runTest {
        val due = Date(System.currentTimeMillis() + HOUR)
        taskDao.tasks.add(recurringTask().copy(fechaVencimiento = due))
        reminderDao.reminders.add(
            Reminder(id = 7L, taskId = 1L, titulo = "R", fechaRecordatorio = due, fechaCreacion = Date())
        )

        assertTrue(useCase.execute(taskDao.tasks[0]))

        val clone = taskDao.tasks.single { it.estado == TaskEstado.PENDIENTE }
        val inherited = reminderDao.reminders.single { it.taskId == clone.id }
        assertEquals(clone.fechaVencimiento, inherited.fechaRecordatorio)
        assertTrue(inherited.activo)
        assertEquals(listOf(inherited.id), scheduler.scheduled.map { it.id })
    }

    @Test
    fun `inherited reminder still in the past is stored but not scheduled`() = runTest {
        // Tarea diaria completada con 3 días de retraso: un día más sigue en el pasado
        val overdue = Date(System.currentTimeMillis() - 3 * DAY)
        taskDao.tasks.add(recurringTask().copy(fechaVencimiento = overdue))
        reminderDao.reminders.add(
            Reminder(id = 7L, taskId = 1L, titulo = "R", fechaRecordatorio = overdue, fechaCreacion = Date())
        )

        assertTrue(useCase.execute(taskDao.tasks[0]))

        val clone = taskDao.tasks.single { it.estado == TaskEstado.PENDIENTE }
        assertEquals(1, reminderDao.reminders.count { it.taskId == clone.id })
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun `non-recurring task does not create reminders`() = runTest {
        taskDao.tasks.add(Task(id = 1L, titulo = "Simple", estado = TaskEstado.PENDIENTE))
        reminderDao.reminders.add(
            Reminder(id = 7L, taskId = 1L, titulo = "R", fechaRecordatorio = Date(), fechaCreacion = Date())
        )

        useCase.execute(taskDao.tasks[0])

        assertEquals(listOf(7L), reminderDao.reminders.map { it.id })
        assertTrue(scheduler.scheduled.isEmpty())
    }

    private companion object {
        const val HOUR = 60 * 60 * 1000L
        const val DAY = 24 * HOUR
    }
}
