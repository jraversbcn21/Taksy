package com.example.taksy.utils

import com.example.taksy.data.Category
import com.example.taksy.data.Reminder
import com.example.taksy.data.Subtask
import com.example.taksy.data.Task
import com.example.taksy.data.TaskEstado
import com.example.taksy.data.TaskPrioridad
import com.example.taksy.data.TipoRecordatorio
import com.example.taksy.utils.BackupManager.ImportErrorType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.Date

class BackupManagerTest {

    private val fixedDate = Date(1700000000000L)

    private fun sampleData() = BackupManager.BackupData(
        categories = listOf(
            Category(id = 1, nombre = "Trabajo", color = "#2196F3", icono = "work", orden = 1),
            Category(id = 2, nombre = "Personal", color = "#4CAF50")
        ),
        tasks = listOf(
            Task(id = 10, titulo = "Tarea 1", fechaCreacion = fixedDate, categoriaId = 1, prioridad = TaskPrioridad.ALTA),
            Task(id = 11, titulo = "Tarea 2", fechaCreacion = fixedDate, fechaVencimiento = fixedDate, estado = TaskEstado.COMPLETADA)
        ),
        subtasks = listOf(
            Subtask(id = 100, taskId = 10, titulo = "Sub 1"),
            Subtask(id = 101, taskId = 10, titulo = "Sub 2", estado = TaskEstado.COMPLETADA)
        ),
        reminders = listOf(
            Reminder(
                id = 200, taskId = 10, titulo = "Recordar",
                descripcion = "Desc", fechaRecordatorio = fixedDate,
                tipoRecordatorio = TipoRecordatorio.DIARIO, fechaCreacion = fixedDate
            )
        )
    )

    // ── Round-trip ─────────────────────────────────────────────────────────

    @Test
    fun `round trip preserves all data`() {
        val original = sampleData()
        val json = BackupManager.exportToJson(original)
        val imported = BackupManager.importFromJson(json)

        assertEquals(original.categories.size, imported.categories.size)
        assertEquals(original.tasks.size, imported.tasks.size)
        assertEquals(original.subtasks.size, imported.subtasks.size)
        assertEquals(original.reminders.size, imported.reminders.size)
    }

    @Test
    fun `round trip preserves category fields`() {
        val json = BackupManager.exportToJson(sampleData())
        val cat = BackupManager.importFromJson(json).categories[0]

        assertEquals(1L, cat.id)
        assertEquals("Trabajo", cat.nombre)
        assertEquals("#2196F3", cat.color)
        assertEquals("work", cat.icono)
        assertEquals(1, cat.orden)
    }

    @Test
    fun `round trip preserves task fields`() {
        val json = BackupManager.exportToJson(sampleData())
        val imported = BackupManager.importFromJson(json)

        val task1 = imported.tasks[0]
        assertEquals(10L, task1.id)
        assertEquals("Tarea 1", task1.titulo)
        assertEquals(1L, task1.categoriaId)
        assertEquals(TaskEstado.PENDIENTE, task1.estado)
        assertEquals(TaskPrioridad.ALTA, task1.prioridad)

        val task2 = imported.tasks[1]
        assertEquals(TaskEstado.COMPLETADA, task2.estado)
        assertTrue(task2.fechaVencimiento != null)
    }

    @Test
    fun `round trip preserves null categoriaId`() {
        val json = BackupManager.exportToJson(sampleData())
        val task2 = BackupManager.importFromJson(json).tasks[1]
        assertNull(task2.categoriaId)
    }

    @Test
    fun `round trip preserves subtask fields`() {
        val json = BackupManager.exportToJson(sampleData())
        val subs = BackupManager.importFromJson(json).subtasks

        assertEquals(100L, subs[0].id)
        assertEquals(10L, subs[0].taskId)
        assertEquals("Sub 1", subs[0].titulo)
        assertEquals(TaskEstado.PENDIENTE, subs[0].estado)
        assertEquals(TaskEstado.COMPLETADA, subs[1].estado)
    }

    @Test
    fun `round trip preserves reminder fields`() {
        val json = BackupManager.exportToJson(sampleData())
        val rem = BackupManager.importFromJson(json).reminders[0]

        assertEquals(200L, rem.id)
        assertEquals(10L, rem.taskId)
        assertEquals("Recordar", rem.titulo)
        assertEquals("Desc", rem.descripcion)
        assertEquals(true, rem.activo)
        assertEquals(TipoRecordatorio.DIARIO, rem.tipoRecordatorio)
    }

    @Test
    fun `round trip with null reminder description`() {
        val data = BackupManager.BackupData(
            categories = emptyList(), tasks = emptyList(), subtasks = emptyList(),
            reminders = listOf(
                Reminder(id = 1, taskId = 1, titulo = "R", fechaRecordatorio = fixedDate, fechaCreacion = fixedDate)
            )
        )
        val json = BackupManager.exportToJson(data)
        val rem = BackupManager.importFromJson(json).reminders[0]
        assertNull(rem.descripcion)
    }

    @Test
    fun `round trip with empty lists`() {
        val data = BackupManager.BackupData(emptyList(), emptyList(), emptyList(), emptyList())
        val json = BackupManager.exportToJson(data)
        val imported = BackupManager.importFromJson(json)

        assertTrue(imported.categories.isEmpty())
        assertTrue(imported.tasks.isEmpty())
        assertTrue(imported.subtasks.isEmpty())
        assertTrue(imported.reminders.isEmpty())
    }

    @Test
    fun `export includes metadata fields`() {
        val json = BackupManager.exportToJson(sampleData())
        assertTrue(json.contains("\"backupVersion\""))
        assertTrue(json.contains("\"exportDate\""))
        assertTrue(json.contains("\"appName\""))
    }

    // ── INVALID_JSON ──────────────────────────────────────────────────────

    @Test
    fun `import throws INVALID_JSON for garbage input`() {
        assertImportError("not json at all", ImportErrorType.INVALID_JSON)
    }

    @Test
    fun `import throws INVALID_JSON for empty string`() {
        assertImportError("", ImportErrorType.INVALID_JSON)
    }

    @Test
    fun `import throws INVALID_JSON for array instead of object`() {
        assertImportError("[1,2,3]", ImportErrorType.INVALID_JSON)
    }

    // ── MISSING_SECTION ───────────────────────────────────────────────────

    @Test
    fun `import throws MISSING_SECTION when categories missing`() {
        val json = """{"tasks":[],"subtasks":[],"reminders":[]}"""
        assertImportError(json, ImportErrorType.MISSING_SECTION, "categories")
    }

    @Test
    fun `import throws MISSING_SECTION when tasks missing`() {
        val json = """{"categories":[],"subtasks":[],"reminders":[]}"""
        assertImportError(json, ImportErrorType.MISSING_SECTION, "tasks")
    }

    @Test
    fun `import throws MISSING_SECTION when subtasks missing`() {
        val json = """{"categories":[],"tasks":[],"reminders":[]}"""
        assertImportError(json, ImportErrorType.MISSING_SECTION, "subtasks")
    }

    @Test
    fun `import throws MISSING_SECTION when reminders missing`() {
        val json = """{"categories":[],"tasks":[],"subtasks":[]}"""
        assertImportError(json, ImportErrorType.MISSING_SECTION, "reminders")
    }

    // ── INVALID_CATEGORY ──────────────────────────────────────────────────

    @Test
    fun `import throws INVALID_CATEGORY for missing nombre`() {
        val json = """{"categories":[{"id":1,"color":"#FFF"}],"tasks":[],"subtasks":[],"reminders":[]}"""
        assertImportError(json, ImportErrorType.INVALID_CATEGORY, "#1")
    }

    @Test
    fun `import throws INVALID_CATEGORY for missing color`() {
        val json = """{"categories":[{"id":1,"nombre":"X"}],"tasks":[],"subtasks":[],"reminders":[]}"""
        assertImportError(json, ImportErrorType.INVALID_CATEGORY, "#1")
    }

    // ── INVALID_TASK ──────────────────────────────────────────────────────

    @Test
    fun `import throws INVALID_TASK for missing titulo`() {
        val json = """{"categories":[],"tasks":[{"id":1,"fechaCreacion":"2023-01-01T00:00:00.000","estado":"PENDIENTE"}],"subtasks":[],"reminders":[]}"""
        assertImportError(json, ImportErrorType.INVALID_TASK, "#1")
    }

    @Test
    fun `import throws INVALID_TASK for missing fechaCreacion`() {
        val json = """{"categories":[],"tasks":[{"id":1,"titulo":"X","estado":"PENDIENTE"}],"subtasks":[],"reminders":[]}"""
        assertImportError(json, ImportErrorType.INVALID_TASK, "#1")
    }

    // ── INVALID_SUBTASK ───────────────────────────────────────────────────

    @Test
    fun `import throws INVALID_SUBTASK for missing titulo`() {
        val json = """{"categories":[],"tasks":[],"subtasks":[{"id":1,"taskId":1,"estado":"PENDIENTE"}],"reminders":[]}"""
        assertImportError(json, ImportErrorType.INVALID_SUBTASK, "#1")
    }

    @Test
    fun `import throws INVALID_SUBTASK for invalid estado`() {
        val json = """{"categories":[],"tasks":[],"subtasks":[{"id":1,"taskId":1,"titulo":"X","estado":"INVALID"}],"reminders":[]}"""
        assertImportError(json, ImportErrorType.INVALID_SUBTASK, "#1")
    }

    // ── INVALID_REMINDER ──────────────────────────────────────────────────

    @Test
    fun `import throws INVALID_REMINDER for missing fields`() {
        val json = """{"categories":[],"tasks":[],"subtasks":[],"reminders":[{"id":1}]}"""
        assertImportError(json, ImportErrorType.INVALID_REMINDER, "#1")
    }

    @Test
    fun `import throws INVALID_REMINDER for invalid tipoRecordatorio`() {
        val json = """{"categories":[],"tasks":[],"subtasks":[],"reminders":[{"id":1,"taskId":1,"titulo":"R","fechaRecordatorio":"2023-01-01T00:00:00.000","activo":true,"tipoRecordatorio":"INVALID","fechaCreacion":"2023-01-01T00:00:00.000"}]}"""
        assertImportError(json, ImportErrorType.INVALID_REMINDER, "#1")
    }

    // ── Malformed dates ─────────────────────────────────────────────────

    @Test
    fun `import throws INVALID_TASK for malformed fechaCreacion`() {
        val json = """{"categories":[],"tasks":[{"id":1,"titulo":"X","fechaCreacion":"not-a-date","estado":"PENDIENTE"}],"subtasks":[],"reminders":[]}"""
        assertImportError(json, ImportErrorType.INVALID_TASK, "#1")
    }

    // ── Priority edge cases ───────────────────────────────────────────────

    @Test
    fun `import defaults to NINGUNA for unknown priority`() {
        val json = BackupManager.exportToJson(sampleData())
        val modified = json.replace("\"ALTA\"", "\"UNKNOWN_PRIORITY\"")
        val imported = BackupManager.importFromJson(modified)
        assertEquals(TaskPrioridad.NINGUNA, imported.tasks[0].prioridad)
    }

    @Test
    fun `import defaults to NINGUNA when prioridad field missing`() {
        val json = """{"categories":[],"tasks":[{"id":1,"titulo":"X","fechaCreacion":"2023-01-01T00:00:00.000","fechaVencimiento":null,"categoriaId":null,"estado":"PENDIENTE"}],"subtasks":[],"reminders":[]}"""
        val imported = BackupManager.importFromJson(json)
        assertEquals(TaskPrioridad.NINGUNA, imported.tasks[0].prioridad)
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun assertImportError(json: String, expectedType: ImportErrorType, detailContains: String? = null) {
        try {
            BackupManager.importFromJson(json)
            fail("Expected BackupImportException with type $expectedType")
        } catch (e: BackupManager.BackupImportException) {
            assertEquals(expectedType, e.errorType)
            if (detailContains != null) {
                assertTrue(
                    "Expected detail containing '$detailContains' but got '${e.detail}'",
                    e.detail?.contains(detailContains) == true
                )
            }
        }
    }
}
