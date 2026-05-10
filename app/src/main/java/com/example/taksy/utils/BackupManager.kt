package com.example.taksy.utils

import com.example.taksy.data.Category
import com.example.taksy.data.Reminder
import com.example.taksy.data.Subtask
import com.example.taksy.data.Task
import com.example.taksy.data.TaskEstado
import com.example.taksy.data.TaskPrioridad
import com.example.taksy.data.TaskRecurrencia
import com.example.taksy.data.TipoRecordatorio
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {

    private const val BACKUP_VERSION = 1
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US)

    enum class ImportErrorType {
        INVALID_JSON,
        MISSING_SECTION,
        INVALID_CATEGORY,
        INVALID_TASK,
        INVALID_SUBTASK,
        INVALID_REMINDER,
        INVALID_DATE
    }

    class BackupImportException(
        val errorType: ImportErrorType,
        val detail: String? = null
    ) : Exception("$errorType${detail?.let { ": $it" } ?: ""}")

    data class BackupData(
        val categories: List<Category>,
        val tasks: List<Task>,
        val subtasks: List<Subtask>,
        val reminders: List<Reminder>
    )

    fun exportToJson(data: BackupData): String {
        val root = JSONObject()
        root.put("backupVersion", BACKUP_VERSION)
        root.put("exportDate", dateFormat.format(Date()))
        root.put("appName", "Taksy")

        root.put("categories", JSONArray().apply {
            data.categories.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("nombre", c.nombre)
                    put("color", c.color)
                    put("icono", c.icono)
                    put("orden", c.orden)
                })
            }
        })

        root.put("tasks", JSONArray().apply {
            data.tasks.forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id)
                    put("titulo", t.titulo)
                    put("descripcion", t.descripcion ?: JSONObject.NULL)
                    put("fechaCreacion", dateFormat.format(t.fechaCreacion))
                    put("fechaVencimiento", t.fechaVencimiento?.let { dateFormat.format(it) })
                    put("categoriaId", t.categoriaId ?: JSONObject.NULL)
                    put("estado", t.estado.name)
                    put("prioridad", t.prioridad.name)
                    put("archivada", t.archivada)
                    put("orden", t.orden)
                    put("recurrencia", t.recurrencia.name)
                    put("fechaCompletada", t.fechaCompletada?.let { dateFormat.format(it) })
                })
            }
        })

        root.put("subtasks", JSONArray().apply {
            data.subtasks.forEach { s ->
                put(JSONObject().apply {
                    put("id", s.id)
                    put("taskId", s.taskId)
                    put("titulo", s.titulo)
                    put("estado", s.estado.name)
                })
            }
        })

        root.put("reminders", JSONArray().apply {
            data.reminders.forEach { r ->
                put(JSONObject().apply {
                    put("id", r.id)
                    put("taskId", r.taskId)
                    put("titulo", r.titulo)
                    put("descripcion", r.descripcion ?: JSONObject.NULL)
                    put("fechaRecordatorio", dateFormat.format(r.fechaRecordatorio))
                    put("activo", r.activo)
                    put("tipoRecordatorio", r.tipoRecordatorio.name)
                    put("fechaCreacion", dateFormat.format(r.fechaCreacion))
                })
            }
        })

        return root.toString(2)
    }

    fun importFromJson(json: String): BackupData {
        val root = try {
            JSONObject(json)
        } catch (_: Exception) {
            throw BackupImportException(ImportErrorType.INVALID_JSON)
        }

        val requiredSections = listOf("categories", "tasks", "subtasks", "reminders")
        for (section in requiredSections) {
            if (!root.has(section)) {
                throw BackupImportException(ImportErrorType.MISSING_SECTION, section)
            }
        }

        val categories = parseCategories(root.getJSONArray("categories"))
        val tasks = parseTasks(root.getJSONArray("tasks"))
        val subtasks = parseSubtasks(root.getJSONArray("subtasks"))
        val reminders = parseReminders(root.getJSONArray("reminders"))

        return BackupData(categories, tasks, subtasks, reminders)
    }

    private fun parseCategories(array: JSONArray): List<Category> {
        val result = mutableListOf<Category>()
        for (i in 0 until array.length()) {
            try {
                val c = array.getJSONObject(i)
                result.add(
                    Category(
                        id = c.getLong("id"),
                        nombre = c.getString("nombre"),
                        color = c.getString("color"),
                        icono = c.optString("icono", "label"),
                        orden = c.optInt("orden", 0)
                    )
                )
            } catch (_: Exception) {
                throw BackupImportException(ImportErrorType.INVALID_CATEGORY, "#${i + 1}")
            }
        }
        return result
    }

    private fun parseTasks(array: JSONArray): List<Task> {
        val result = mutableListOf<Task>()
        for (i in 0 until array.length()) {
            try {
                val t = array.getJSONObject(i)
                val titulo = t.getString("titulo")
                val fechaCreacionStr = t.getString("fechaCreacion")
                val fechaCreacion = dateFormat.parse(fechaCreacionStr)
                    ?: throw BackupImportException(ImportErrorType.INVALID_DATE, "fechaCreacion: $fechaCreacionStr")
                result.add(
                    Task(
                        id = t.getLong("id"),
                        titulo = titulo,
                        descripcion = if (t.has("descripcion") && !t.isNull("descripcion")) t.getString("descripcion") else null,
                        fechaCreacion = fechaCreacion,
                        fechaVencimiento = if (t.isNull("fechaVencimiento")) null
                            else dateFormat.parse(t.getString("fechaVencimiento")),
                        categoriaId = if (t.isNull("categoriaId")) null else t.getLong("categoriaId"),
                        estado = TaskEstado.valueOf(t.getString("estado")),
                        prioridad = try { TaskPrioridad.valueOf(t.optString("prioridad", "NINGUNA")) } catch (_: Exception) { TaskPrioridad.NINGUNA },
                        archivada = t.optBoolean("archivada", false),
                        orden = t.optInt("orden", 0),
                        recurrencia = try { TaskRecurrencia.valueOf(t.optString("recurrencia", "NINGUNA")) } catch (_: Exception) { TaskRecurrencia.NINGUNA },
                        fechaCompletada = if (t.has("fechaCompletada") && !t.isNull("fechaCompletada"))
                            dateFormat.parse(t.getString("fechaCompletada")) else null
                    )
                )
            } catch (e: BackupImportException) {
                throw e
            } catch (_: Exception) {
                throw BackupImportException(ImportErrorType.INVALID_TASK, "#${i + 1}")
            }
        }
        return result
    }

    private fun parseSubtasks(array: JSONArray): List<Subtask> {
        val result = mutableListOf<Subtask>()
        for (i in 0 until array.length()) {
            try {
                val s = array.getJSONObject(i)
                result.add(
                    Subtask(
                        id = s.getLong("id"),
                        taskId = s.getLong("taskId"),
                        titulo = s.getString("titulo"),
                        estado = TaskEstado.valueOf(s.getString("estado"))
                    )
                )
            } catch (_: Exception) {
                throw BackupImportException(ImportErrorType.INVALID_SUBTASK, "#${i + 1}")
            }
        }
        return result
    }

    private fun parseReminders(array: JSONArray): List<Reminder> {
        val result = mutableListOf<Reminder>()
        for (i in 0 until array.length()) {
            try {
                val r = array.getJSONObject(i)
                result.add(
                    Reminder(
                        id = r.getLong("id"),
                        taskId = r.getLong("taskId"),
                        titulo = r.getString("titulo"),
                        descripcion = if (r.isNull("descripcion")) null else r.getString("descripcion"),
                        fechaRecordatorio = dateFormat.parse(r.getString("fechaRecordatorio")) ?: throw Exception(),
                        activo = r.getBoolean("activo"),
                        tipoRecordatorio = TipoRecordatorio.valueOf(r.getString("tipoRecordatorio")),
                        fechaCreacion = dateFormat.parse(r.getString("fechaCreacion")) ?: throw Exception()
                    )
                )
            } catch (_: Exception) {
                throw BackupImportException(ImportErrorType.INVALID_REMINDER, "#${i + 1}")
            }
        }
        return result
    }
}
