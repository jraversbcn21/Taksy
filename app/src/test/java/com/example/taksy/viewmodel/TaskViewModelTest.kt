package com.example.taksy.viewmodel

import android.app.Application
import android.content.Context
import com.example.taksy.data.Reminder
import com.example.taksy.data.ReminderDao
import com.example.taksy.data.Subtask
import com.example.taksy.data.SubtaskDao
import com.example.taksy.data.Task
import com.example.taksy.data.TaskDao
import com.example.taksy.data.TaskEstado
import com.example.taksy.data.TaskPrioridad
import com.example.taksy.data.TipoRecordatorio
import com.example.taksy.repository.TaskFilter
import com.example.taksy.repository.TaskRepository
import com.example.taksy.service.ReminderSchedulerContract
import com.example.taksy.widget.TaskWidgetProvider
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.Runs
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeTaskDao: FakeTaskDao
    private lateinit var fakeSubtaskDao: FakeSubtaskDao
    private lateinit var fakeReminderDao: FakeReminderDao
    private lateinit var repo: TaskRepository
    private lateinit var fakeScheduler: FakeReminderScheduler
    private lateinit var viewModel: TaskViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(TaskWidgetProvider.Companion)
        every { TaskWidgetProvider.refreshAll(any()) } just Runs

        fakeTaskDao = FakeTaskDao()
        fakeSubtaskDao = FakeSubtaskDao()
        fakeReminderDao = FakeReminderDao()
        repo = TaskRepository(mockk(relaxed = true), fakeTaskDao, fakeSubtaskDao, fakeReminderDao)
        fakeScheduler = FakeReminderScheduler()

        val mockApp = mockk<Application>(relaxed = true)
        val mockContext = mockk<Context>(relaxed = true)
        every { mockApp.applicationContext } returns mockContext

        viewModel = TaskViewModel(repo, fakeScheduler, mockApp)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // ── Filter switching ──────────────────────────────────────────────────

    @Test
    fun `initial filter is TODAS`() {
        assertEquals(TaskFilter.TODAS, viewModel.uiState.value.currentFilter)
    }

    @Test
    fun `setFilter updates currentFilter in state`() {
        viewModel.setFilter(TaskFilter.PENDIENTES)
        assertEquals(TaskFilter.PENDIENTES, viewModel.uiState.value.currentFilter)
    }

    @Test
    fun `setFilter to PENDIENTES shows only pending tasks`() = runTest {
        fakeTaskDao.tasks.add(Task(id = 1, titulo = "Pending", estado = TaskEstado.PENDIENTE))
        fakeTaskDao.tasks.add(Task(id = 2, titulo = "Done", estado = TaskEstado.COMPLETADA))

        viewModel.setFilter(TaskFilter.PENDIENTES)
        advanceUntilIdle()

        val tasks = viewModel.uiState.value.tasks
        assertTrue(tasks.all { it.estado == TaskEstado.PENDIENTE })
    }

    // ── Add task ──────────────────────────────────────────────────────────

    @Test
    fun `addTask ignores blank title`() = runTest {
        viewModel.addTask("   ")
        advanceUntilIdle()
        assertTrue(fakeTaskDao.tasks.isEmpty())
    }

    @Test
    fun `addTask trims title and stores`() = runTest {
        viewModel.addTask("  My task  ")
        advanceUntilIdle()
        assertEquals("My task", fakeTaskDao.tasks[0].titulo)
    }

    @Test
    fun `addTask with priority stores correct priority`() = runTest {
        viewModel.addTask("High", null, null, TaskPrioridad.ALTA)
        advanceUntilIdle()
        assertEquals(TaskPrioridad.ALTA, fakeTaskDao.tasks[0].prioridad)
    }

    // ── Toggle task status ────────────────────────────────────────────────

    @Test
    fun `toggleTaskStatus flips PENDIENTE to COMPLETADA`() = runTest {
        val task = Task(id = 1, titulo = "T", estado = TaskEstado.PENDIENTE)
        fakeTaskDao.tasks.add(task)

        viewModel.toggleTaskStatus(task)
        advanceUntilIdle()

        assertEquals(TaskEstado.COMPLETADA, fakeTaskDao.tasks[0].estado)
    }

    @Test
    fun `toggleTaskStatus cancels reminders when completing`() = runTest {
        val task = Task(id = 1, titulo = "T", estado = TaskEstado.PENDIENTE)
        fakeTaskDao.tasks.add(task)
        val reminder = Reminder(id = 5, taskId = 1, titulo = "R", fechaRecordatorio = Date(), fechaCreacion = Date())
        fakeReminderDao.reminders.add(reminder)

        viewModel.toggleTaskStatus(task)
        advanceUntilIdle()

        assertTrue(fakeScheduler.cancelledIds.contains(5L))
    }

    // ── Auto-complete ─────────────────────────────────────────────────────

    @Test
    fun `toggleSubtask auto-completes parent when all subtasks done`() = runTest {
        val task = Task(id = 1, titulo = "Parent", estado = TaskEstado.PENDIENTE)
        fakeTaskDao.tasks.add(task)
        val sub = Subtask(id = 10, taskId = 1, titulo = "Sub", estado = TaskEstado.PENDIENTE)
        fakeSubtaskDao.subtaskList.add(sub)

        viewModel.toggleSubtaskStatus(sub)
        advanceUntilIdle()

        assertEquals(TaskEstado.COMPLETADA, fakeSubtaskDao.subtaskList[0].estado)
        assertEquals(TaskEstado.COMPLETADA, fakeTaskDao.tasks[0].estado)
    }

    @Test
    fun `toggleSubtask does not auto-complete when other subtasks pending`() = runTest {
        val task = Task(id = 1, titulo = "Parent", estado = TaskEstado.PENDIENTE)
        fakeTaskDao.tasks.add(task)
        fakeSubtaskDao.subtaskList.add(Subtask(id = 10, taskId = 1, titulo = "Sub1", estado = TaskEstado.PENDIENTE))
        fakeSubtaskDao.subtaskList.add(Subtask(id = 11, taskId = 1, titulo = "Sub2", estado = TaskEstado.PENDIENTE))

        viewModel.toggleSubtaskStatus(fakeSubtaskDao.subtaskList[0])
        advanceUntilIdle()

        assertEquals(TaskEstado.PENDIENTE, fakeTaskDao.tasks[0].estado)
    }

    // ── Quick reminders ───────────────────────────────────────────────────

    @Test
    fun `setQuickReminder deletes existing and creates new`() = runTest {
        val existing = Reminder(id = 5, taskId = 1, titulo = "Old", fechaRecordatorio = Date(), fechaCreacion = Date())
        fakeReminderDao.reminders.add(existing)

        val futureDate = Date(System.currentTimeMillis() + 3600000)
        viewModel.setQuickReminder(1, "Task Title", futureDate)
        advanceUntilIdle()

        assertTrue(fakeScheduler.cancelledIds.contains(5L))
        assertEquals(1, fakeReminderDao.reminders.size)
        assertEquals("Task Title", fakeReminderDao.reminders[0].titulo)
        assertEquals(1, fakeScheduler.scheduledReminders.size)
    }

    @Test
    fun `deleteReminderForTask cancels and removes all`() = runTest {
        fakeReminderDao.reminders.add(
            Reminder(id = 10, taskId = 1, titulo = "R1", fechaRecordatorio = Date(), fechaCreacion = Date())
        )
        fakeReminderDao.reminders.add(
            Reminder(id = 11, taskId = 1, titulo = "R2", fechaRecordatorio = Date(), fechaCreacion = Date())
        )

        viewModel.deleteReminderForTask(1)
        advanceUntilIdle()

        assertTrue(fakeScheduler.cancelledIds.containsAll(listOf(10L, 11L)))
        assertTrue(fakeReminderDao.reminders.isEmpty())
    }

    // ── Search ────────────────────────────────────────────────────────────

    @Test
    fun `searchAllTasks returns matching tasks`() = runTest {
        fakeTaskDao.tasks.add(Task(id = 1, titulo = "Buy groceries"))
        fakeTaskDao.tasks.add(Task(id = 2, titulo = "Clean house"))

        val results = viewModel.searchAllTasks("buy").first()

        assertEquals(1, results.size)
        assertEquals("Buy groceries", results[0].titulo)
    }

    // ── Error handling ────────────────────────────────────────────────────

    @Test
    fun `clearError resets error state`() {
        viewModel.clearError()
        assertEquals(null, viewModel.uiState.value.error)
    }

    // ── Fakes ─────────────────────────────────────────────────────────────

    class FakeTaskDao : TaskDao {
        val tasks = mutableListOf<Task>()
        private val tasksFlow = MutableStateFlow<List<Task>>(emptyList())

        private fun emit() { tasksFlow.value = tasks.toList() }

        override fun getAllTasks(): Flow<List<Task>> = tasksFlow
        override fun getPendingTasks(): Flow<List<Task>> = flowOf(tasks.filter { it.estado == TaskEstado.PENDIENTE })
        override fun getCompletedTasks(): Flow<List<Task>> = flowOf(tasks.filter { it.estado == TaskEstado.COMPLETADA })
        override fun getTasksByCategory(categoryId: Long): Flow<List<Task>> = flowOf(tasks.filter { it.categoriaId == categoryId })
        override fun getTasksWithoutCategory(): Flow<List<Task>> = flowOf(tasks.filter { it.categoriaId == null })
        override fun getReallyPendingTasks(): Flow<List<Task>> = flowOf(tasks.filter { it.estado == TaskEstado.PENDIENTE })
        override fun getReallyCompletedTasks(): Flow<List<Task>> = flowOf(tasks.filter { it.estado == TaskEstado.COMPLETADA })
        override fun searchTasksByCategory(categoryId: Long, query: String): Flow<List<Task>> =
            flowOf(tasks.filter { it.categoriaId == categoryId && it.titulo.contains(query, ignoreCase = true) })
        override suspend fun getTaskById(id: Long): Task? = tasks.find { it.id == id }
        override suspend fun insertTask(task: Task): Long { tasks.add(task); emit(); return task.id }
        override suspend fun updateTask(task: Task) {
            val idx = tasks.indexOfFirst { it.id == task.id }
            if (idx >= 0) tasks[idx] = task
            emit()
        }
        override suspend fun deleteTask(task: Task) { tasks.removeIf { it.id == task.id }; emit() }
        override suspend fun markTaskAsCompleted(id: Long) {}
        override suspend fun markTaskAsPending(id: Long) {}
        override suspend fun deleteCompletedTasks() { tasks.removeIf { it.estado == TaskEstado.COMPLETADA }; emit() }
        override suspend fun getTasksDueToday(startOfDay: Long, endOfDay: Long): List<Task> = emptyList()
        override suspend fun getTasksDueSoon(now: Long, nextWeek: Long): List<Task> = emptyList()
        override suspend fun getOverdueTasks(now: Long): List<Task> = emptyList()
        override suspend fun getAllTasksSync(): List<Task> = tasks.toList()
        override fun getPendingTasksSync(): List<Task> = tasks.filter { it.estado == TaskEstado.PENDIENTE }
        override suspend fun deleteAllTasks() { tasks.clear(); emit() }
        override fun searchAllTasks(query: String): Flow<List<Task>> =
            flowOf(tasks.filter { it.titulo.contains(query, ignoreCase = true) })
        override fun getArchivedTasksByCategory(categoryId: Long): Flow<List<Task>> =
            flowOf(tasks.filter { it.categoriaId == categoryId && it.archivada })
        override suspend fun archiveTask(taskId: Long) {
            val idx = tasks.indexOfFirst { it.id == taskId }
            if (idx >= 0) tasks[idx] = tasks[idx].copy(archivada = true)
            emit()
        }
        override suspend fun unarchiveTask(taskId: Long) {
            val idx = tasks.indexOfFirst { it.id == taskId }
            if (idx >= 0) tasks[idx] = tasks[idx].copy(archivada = false)
            emit()
        }
        override suspend fun updateTasks(tasks: List<Task>) {
            tasks.forEach { updateTask(it) }
        }
    }

    class FakeSubtaskDao : SubtaskDao {
        val subtaskList = mutableListOf<Subtask>()

        override fun getSubtasksByTaskId(taskId: Long): Flow<List<Subtask>> =
            flowOf(subtaskList.filter { it.taskId == taskId })
        override suspend fun insertSubtask(subtask: Subtask): Long { subtaskList.add(subtask); return subtask.id }
        override suspend fun updateSubtask(subtask: Subtask) {
            val idx = subtaskList.indexOfFirst { it.id == subtask.id }
            if (idx >= 0) subtaskList[idx] = subtask
        }
        override suspend fun deleteSubtask(subtask: Subtask) { subtaskList.removeIf { it.id == subtask.id } }
        override suspend fun deleteSubtasksByTaskId(taskId: Long) { subtaskList.removeIf { it.taskId == taskId } }
        override suspend fun getAllSubtasksSync(): List<Subtask> = subtaskList.toList()
        override suspend fun deleteAllSubtasks() { subtaskList.clear() }
    }

    class FakeReminderDao : ReminderDao {
        val reminders = mutableListOf<Reminder>()
        private var nextId = 100L

        override fun getRemindersByTaskId(taskId: Long): Flow<List<Reminder>> =
            flowOf(reminders.filter { it.taskId == taskId })
        override suspend fun getActiveRemindersDue(now: Long): List<Reminder> = emptyList()
        override fun getAllActiveReminders(): Flow<List<Reminder>> = flowOf(reminders.filter { it.activo })
        override suspend fun getAllActiveRemindersSync(): List<Reminder> = reminders.filter { it.activo }
        override suspend fun getReminderById(id: Long): Reminder? = reminders.find { it.id == id }
        override suspend fun insertReminder(reminder: Reminder): Long {
            val id = nextId++
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
        override suspend fun getRemindersByTaskIdSync(taskId: Long): List<Reminder> =
            reminders.filter { it.taskId == taskId }
        override suspend fun getAllRemindersSync(): List<Reminder> = reminders.toList()
        override suspend fun deleteAllReminders() { reminders.clear() }
    }

    class FakeReminderScheduler : ReminderSchedulerContract {
        val scheduledReminders = mutableListOf<Reminder>()
        val cancelledIds = mutableListOf<Long>()

        override fun scheduleReminder(reminder: Reminder) { scheduledReminders.add(reminder) }
        override fun cancelReminder(reminderId: Long) { cancelledIds.add(reminderId) }
        override fun cancelAllReminders() {}
    }
}
