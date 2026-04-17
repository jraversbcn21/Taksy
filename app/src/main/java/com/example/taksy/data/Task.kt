package com.example.taksy.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entidad que representa una tarea en la base de datos
 */
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val titulo: String,
    val fechaCreacion: Date = Date(),
    val fechaVencimiento: Date? = null, // Fecha de vencimiento opcional
    val categoriaId: Long? = null, // ID de la categoría (opcional)
    val estado: TaskEstado = TaskEstado.PENDIENTE
)

/**
 * Enum que representa los posibles estados de una tarea
 */
enum class TaskEstado {
    PENDIENTE,
    COMPLETADA
}
