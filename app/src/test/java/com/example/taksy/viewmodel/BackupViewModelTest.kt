package com.example.taksy.viewmodel

import android.content.Context
import androidx.room.withTransaction
import com.example.taksy.data.AppDatabase
import com.example.taksy.data.Category
import com.example.taksy.data.CategoryDao
import com.example.taksy.data.Reminder
import com.example.taksy.data.ReminderDao
import com.example.taksy.data.Subtask
import com.example.taksy.data.SubtaskDao
import com.example.taksy.data.Task
import com.example.taksy.data.TaskDao
import com.example.taksy.data.TaskEstado
import com.example.taksy.service.ReminderSchedulerContract
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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

/**
 * Regression tests: import de backup debe ser atómico. Si una escritura falla
 * a mitad de camino, la transacción debe revertir en vez de dejar la DB vaciada
 * o con datos parciales (mismo patrón de riesgo que el bug de duplicación).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    // ── Fakes ──────────────────────────────────────────────────────────────

    private class FakeTaskDao(private val failOnInsert: Boolean = false) : TaskDao {
        val tasks = mutableListOf<Task>()
        override fun getAllTasks(): Flow<List<Task>> = flowOf(tasks.toList())
        override fun getPendingTasks(): Flow<List<Task>> = flowOf(emptyList())
        override fun getCompletedTasks(): Flow<List<Task>> = flowOf(emptyList())
        override fun getTasksByCategory(categoryId: Long): Flow<List<Task>> = flowOf(emptyList())
        override fun getTasksWithoutCategory(): Flow<List<Task>> = flowOf(emptyList())
        override fun getReallyPendingTasks(): Flow<List<Task>> = flowOf(emptyList())
        override fun getReallyCompletedTasks(): Flow<List<Task>> = flowOf(emptyList())
        override fun searchTasksByCategory(categoryId: Long, query: String): Flow<List<Task>> = flowOf(emptyList())
        override fun searchAllTasks(query: String): Flow<List<Task>> = flowOf(emptyList())
        override suspend fun getTaskById(id: Long): Task? = tasks.find { it.id == id }
        override suspend fun insertTask(task: Task): Long {
            if (failOnInsert) throw RuntimeException("simulated crash mid-import")
            tasks.add(task)
            return task.id
        }
        override suspend fun updateTask(task: Task) {}
        override suspend fun deleteTask(task: Task) {}
        override suspend fun completeTaskIfPending(id: Long, fechaCompletada: java.util.Date): Int = 0
        override suspend fun markTaskAsPending(id: Long) {}
        override suspend fun getAllTasksByCategorySync(categoryId: Long): List<Task> = emptyList()
        override suspend fun getTasksDueToday(startOfDay: Long, endOfDay: Long): List<Task> = emptyList()
        override suspend fun getTasksDueSoon(now: Long, nextWeek: Long): List<Task> = emptyList()
        override suspend fun getOverdueTasks(now: Long): List<Task> = emptyList()
        override suspend fun getAllTasksSync(): List<Task> = tasks.toList()
        override fun getPendingTasksSync(): List<Task> = emptyList()
        override suspend fun deleteAllTasks() { tasks.clear() }
    }

    private class FakeSubtaskDao : SubtaskDao {
        val subtasks = mutableListOf<Subtask>()
        override fun getSubtasksByTaskId(taskId: Long): Flow<List<Subtask>> = flowOf(emptyList())
        override suspend fun insertSubtask(subtask: Subtask): Long { subtasks.add(subtask); return subtask.id }
        override suspend fun updateSubtask(subtask: Subtask) {}
        override suspend fun deleteSubtask(subtask: Subtask) {}
        override suspend fun deleteSubtasksByTaskId(taskId: Long) {}
        override suspend fun getAllSubtasksSync(): List<Subtask> = subtasks.toList()
        override suspend fun deleteAllSubtasks() { subtasks.clear() }
    }

    private class FakeCategoryDao : CategoryDao {
        val categories = mutableListOf<Category>()
        override fun getAllCategories(): Flow<List<Category>> = flowOf(categories.toList())
        override suspend fun getAllCategoriesSync(): List<Category> = categories.toList()
        override suspend fun getCategoryById(id: Long): Category? = categories.find { it.id == id }
        override suspend fun insertCategory(category: Category): Long { categories.add(category); return category.id }
        override suspend fun updateCategory(category: Category) {}
        override suspend fun deleteCategory(category: Category) {}
        override suspend fun getCategoryCount(): Int = categories.size
        override suspend fun deleteAllCategories() { categories.clear() }
        override suspend fun insertCategories(categories: List<Category>) { this.categories.addAll(categories) }
        override suspend fun updateCategoryOrder(categoryId: Long, newOrder: Int) {}
        override suspend fun updateCategories(categories: List<Category>) {}
    }

    private class FakeReminderDao : ReminderDao {
        val reminders = mutableListOf<Reminder>()
        override fun getRemindersByTaskId(taskId: Long): Flow<List<Reminder>> = flowOf(emptyList())
        override suspend fun getActiveRemindersDue(now: Long): List<Reminder> = emptyList()
        override fun getAllActiveReminders(): Flow<List<Reminder>> = flowOf(emptyList())
        override suspend fun getAllActiveRemindersSync(): List<Reminder> = emptyList()
        override suspend fun getReminderById(id: Long): Reminder? = reminders.find { it.id == id }
        override suspend fun insertReminder(reminder: Reminder): Long { reminders.add(reminder); return reminder.id }
        override suspend fun updateReminder(reminder: Reminder) {}
        override suspend fun deleteReminder(reminder: Reminder) {}
        override suspend fun deleteRemindersByTaskId(taskId: Long) {}
        override suspend fun updateReminderStatus(id: Long, activo: Boolean) {}
        override suspend fun deactivateRemindersByTaskId(taskId: Long) {}
        override suspend fun advanceReminderIfUnchanged(id: Long, expectedDate: Date, nextDate: Date): Int = 0
        override suspend fun getRemindersByTaskIdSync(taskId: Long): List<Reminder> = emptyList()
        override suspend fun getAllRemindersSync(): List<Reminder> = reminders.toList()
        override suspend fun deleteAllReminders() { reminders.clear() }
    }

    private class FakeScheduler : ReminderSchedulerContract {
        val scheduled = mutableListOf<Reminder>()
        override fun scheduleReminder(reminder: Reminder) { scheduled.add(reminder) }
        override fun cancelReminder(reminderId: Long) {}
    }

    private lateinit var taskDao: FakeTaskDao
    private lateinit var subtaskDao: FakeSubtaskDao
    private lateinit var categoryDao: FakeCategoryDao
    private lateinit var reminderDao: FakeReminderDao
    private lateinit var scheduler: FakeScheduler

    /** Mockea `AppDatabase.withTransaction` para que ejecute el bloque directamente sobre los fakes. */
    private fun mockTransaction(): AppDatabase {
        mockkStatic("androidx.room.RoomDatabaseKt")
        val db = mockk<AppDatabase>(relaxed = true)
        val blockSlot = slot<suspend () -> Any?>()
        coEvery { db.withTransaction(capture(blockSlot)) } coAnswers { blockSlot.captured.invoke() }
        return db
    }

    private fun makeViewModel(db: AppDatabase): BackupViewModel {
        val context = mockk<Context>(relaxed = true)
        return BackupViewModel(context, db, taskDao, subtaskDao, categoryDao, reminderDao, scheduler)
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        taskDao = FakeTaskDao()
        subtaskDao = FakeSubtaskDao()
        categoryDao = FakeCategoryDao()
        reminderDao = FakeReminderDao()
        scheduler = FakeScheduler()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic("androidx.room.RoomDatabaseKt")
    }

    private fun sampleJson() = """
        {
          "backupVersion": 1,
          "exportDate": "2026-01-01T00:00:00.000",
          "appName": "Taksy",
          "categories": [{"id":1,"nombre":"Trabajo","color":"#2196F3","icono":"work","orden":1}],
          "tasks": [{"id":1,"titulo":"T","descripcion":null,"fechaCreacion":"2026-01-01T00:00:00.000","fechaVencimiento":null,"categoriaId":1,"estado":"PENDIENTE","prioridad":"NINGUNA","archivada":false,"orden":0,"recurrencia":"NINGUNA","fechaCompletada":null}],
          "subtasks": [],
          "reminders": []
        }
    """.trimIndent()

    @Test
    fun `successful import wraps all writes in exactly one transaction call`() = runTest {
        val db = mockTransaction()
        val viewModel = makeViewModel(db)

        viewModel.importBackupJson(sampleJson())
        advanceUntilIdle()

        // Prueba estructural: si alguien vuelve a sacar las escrituras fuera de
        // withTransaction (regresión del bug de import no-atómico), esto falla.
        coVerify(exactly = 1) { db.withTransaction<Unit>(any()) }
        assertEquals(1, categoryDao.categories.size)
        assertEquals(1, taskDao.tasks.size)
        assertTrue(viewModel.backupState.value is BackupState.ImportSuccess)
    }

    @Test
    fun `exception mid-transaction is caught and reported as Error, never as success`() = runTest {
        // Los fakes no reproducen el rollback real de Room (eso lo garantiza
        // withTransaction en producción); lo que este test fija es el contrato del
        // ViewModel: una excepción durante el import nunca debe leerse como
        // ImportSuccess con datos parciales.
        val db = mockTransaction()
        val failingTaskDao = FakeTaskDao(failOnInsert = true)
        val context = mockk<Context>(relaxed = true)
        val viewModel = BackupViewModel(context, db, failingTaskDao, subtaskDao, categoryDao, reminderDao, scheduler)

        viewModel.importBackupJson(sampleJson())
        advanceUntilIdle()

        assertTrue(viewModel.backupState.value is BackupState.Error)
    }

    @Test
    fun `import schedules only future active reminders`() = runTest {
        val db = mockTransaction()
        val viewModel = makeViewModel(db)
        val past = java.util.Date(System.currentTimeMillis() - 3600_000)
        val future = java.util.Date(System.currentTimeMillis() + 3600_000)
        val json = """
            {
              "backupVersion": 1,
              "exportDate": "2026-01-01T00:00:00.000",
              "appName": "Taksy",
              "categories": [],
              "tasks": [],
              "subtasks": [],
              "reminders": [
                {"id":1,"taskId":1,"titulo":"Past","descripcion":null,"fechaRecordatorio":"${dateStr(past)}","activo":true,"tipoRecordatorio":"UNA_VEZ","fechaCreacion":"${dateStr(past)}"},
                {"id":2,"taskId":1,"titulo":"Future","descripcion":null,"fechaRecordatorio":"${dateStr(future)}","activo":true,"tipoRecordatorio":"UNA_VEZ","fechaCreacion":"${dateStr(past)}"}
              ]
            }
        """.trimIndent()

        viewModel.importBackupJson(json)
        advanceUntilIdle()

        assertEquals(1, scheduler.scheduled.size)
        assertEquals("Future", scheduler.scheduled[0].titulo)
    }

    private fun dateStr(date: java.util.Date): String {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", java.util.Locale.US)
        return format.format(date)
    }
}
