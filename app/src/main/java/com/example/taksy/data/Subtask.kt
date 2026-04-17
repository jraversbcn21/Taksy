package com.example.taksy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que representa una subtarea en la base de datos
 */
@Entity(tableName = "subtasks")
data class Subtask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long, // ID de la tarea padre
    val titulo: String,
    val estado: TaskEstado = TaskEstado.PENDIENTE
)
