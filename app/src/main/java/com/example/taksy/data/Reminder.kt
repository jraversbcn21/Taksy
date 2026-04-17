package com.example.taksy.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entidad para recordatorios de tareas
 */
@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long, // ID de la tarea asociada
    val titulo: String, // Título del recordatorio
    val descripcion: String? = null, // Descripción opcional
    val fechaRecordatorio: Date, // Fecha y hora del recordatorio
    val activo: Boolean = true, // Si el recordatorio está activo
    val tipoRecordatorio: TipoRecordatorio = TipoRecordatorio.UNA_VEZ,
    val fechaCreacion: Date = Date()
)

/**
 * Tipos de recordatorios
 */
enum class TipoRecordatorio {
    UNA_VEZ,        // Recordatorio único
    DIARIO,         // Todos los días
    SEMANAL,        // Cada semana
    MENSUAL         // Cada mes
}
