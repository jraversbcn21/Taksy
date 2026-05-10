package com.example.taksy.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val titulo: String,
    val descripcion: String? = null,
    val fechaCreacion: Date = Date(),
    val fechaVencimiento: Date? = null,
    val categoriaId: Long? = null,
    val estado: TaskEstado = TaskEstado.PENDIENTE,
    val prioridad: TaskPrioridad = TaskPrioridad.NINGUNA,
    val archivada: Boolean = false,
    val orden: Int = 0,
    val recurrencia: TaskRecurrencia = TaskRecurrencia.NINGUNA
)

enum class TaskEstado {
    PENDIENTE,
    COMPLETADA
}

enum class TaskPrioridad {
    NINGUNA,
    BAJA,
    MEDIA,
    ALTA
}

enum class TaskRecurrencia {
    NINGUNA,
    DIARIA,
    SEMANAL,
    MENSUAL,
    ANUAL
}
