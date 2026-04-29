package com.example.taksy.utils

import com.example.taksy.data.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {

    private const val BACKUP_VERSION = 1
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US)

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
                    put("fechaCreacion", dateFormat.format(t.fechaCreacion))
                    put("fechaVencimiento", t.fechaVencimiento?.let { dateFormat.format(it) })
                    put("categoriaId", t.categoriaId ?: JSONObject.NULL)
                    put("estado", t.estado.name)
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
        val root = JSONObject(json)

        val categories = mutableListOf<Category>()
        val categoriesArray = root.getJSONArray("categories")
        for (i in 0 until categoriesArray.length()) {
            val c = categoriesArray.getJSONObject(i)
            categories.add(
                Category(
                    id = c.getLong("id"),
                    nombre = c.getString("nombre"),
                    color = c.getString("color"),
                    icono = c.optString("icono", "label"),
                    orden = c.optInt("orden", 0)
                )
            )
        }

        val tasks = mutableListOf<Task>()
        val tasksArray = root.getJSONArray("tasks")
        for (i in 0 until tasksArray.length()) {
            val t = tasksArray.getJSONObject(i)
            tasks.add(
                Task(
                    id = t.getLong("id"),
                    titulo = t.getString("titulo"),
                    fechaCreacion = dateFormat.parse(t.getString("fechaCreacion")) ?: Date(),
                    fechaVencimiento = if (t.isNull("fechaVencimiento")) null
                        else dateFormat.parse(t.getString("fechaVencimiento")),
                    categoriaId = if (t.isNull("categoriaId")) null else t.getLong("categoriaId"),
                    estado = TaskEstado.valueOf(t.getString("estado"))
                )
            )
        }

        val subtasks = mutableListOf<Subtask>()
        val subtasksArray = root.getJSONArray("subtasks")
        for (i in 0 until subtasksArray.length()) {
            val s = subtasksArray.getJSONObject(i)
            subtasks.add(
                Subtask(
                    id = s.getLong("id"),
                    taskId = s.getLong("taskId"),
                    titulo = s.getString("titulo"),
                    estado = TaskEstado.valueOf(s.getString("estado"))
                )
            )
        }

        val reminders = mutableListOf<Reminder>()
        val remindersArray = root.getJSONArray("reminders")
        for (i in 0 until remindersArray.length()) {
            val r = remindersArray.getJSONObject(i)
            reminders.add(
                Reminder(
                    id = r.getLong("id"),
                    taskId = r.getLong("taskId"),
                    titulo = r.getString("titulo"),
                    descripcion = if (r.isNull("descripcion")) null else r.getString("descripcion"),
                    fechaRecordatorio = dateFormat.parse(r.getString("fechaRecordatorio")) ?: Date(),
                    activo = r.getBoolean("activo"),
                    tipoRecordatorio = TipoRecordatorio.valueOf(r.getString("tipoRecordatorio")),
                    fechaCreacion = dateFormat.parse(r.getString("fechaCreacion")) ?: Date()
                )
            )
        }

        return BackupData(categories, tasks, subtasks, reminders)
    }
}
