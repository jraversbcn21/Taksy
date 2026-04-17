package com.example.taksy.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Utilidades para manejo de fechas
 */
object DateUtils {
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
    
    /**
     * Formatea una fecha para mostrar
     */
    fun formatDate(date: Date): String {
        return dateFormat.format(date)
    }
    
    /**
     * Formatea una fecha de forma corta (solo día y mes)
     */
    fun formatShortDate(date: Date): String {
        return shortDateFormat.format(date)
    }
    
    /**
     * Obtiene la fecha de hoy
     */
    fun getToday(): Date {
        return Date()
    }
    
    /**
     * Obtiene la fecha de mañana
     */
    fun getTomorrow(): Date {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        return calendar.time
    }
    
    /**
     * Obtiene la fecha de la próxima semana
     */
    fun getNextWeek(): Date {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 7)
        return calendar.time
    }
    
    /**
     * Verifica si una fecha es hoy
     */
    fun isToday(date: Date): Boolean {
        val today = getToday()
        return dateFormat.format(date) == dateFormat.format(today)
    }
    
    /**
     * Verifica si una fecha es mañana
     */
    fun isTomorrow(date: Date): Boolean {
        val tomorrow = getTomorrow()
        return dateFormat.format(date) == dateFormat.format(tomorrow)
    }
    
    /**
     * Verifica si una fecha está vencida
     */
    fun isOverdue(date: Date): Boolean {
        val today = getToday()
        return date.before(today) && !isToday(date)
    }
    
    /**
     * Verifica si una fecha vence pronto (próximos 7 días)
     */
    fun isDueSoon(date: Date): Boolean {
        val today = getToday()
        val nextWeek = getNextWeek()
        return date.after(today) && date.before(nextWeek) || isToday(date) || isTomorrow(date)
    }
    
    /**
     * Obtiene el estado de vencimiento de una fecha
     */
    fun getDueDateStatus(date: Date): DueDateStatus {
        return when {
            isOverdue(date) -> DueDateStatus.OVERDUE
            isToday(date) -> DueDateStatus.DUE_TODAY
            isTomorrow(date) -> DueDateStatus.DUE_TOMORROW
            isDueSoon(date) -> DueDateStatus.DUE_SOON
            else -> DueDateStatus.NORMAL
        }
    }
}

/**
 * Estados de vencimiento de una fecha
 */
enum class DueDateStatus {
    NORMAL,      // Fecha normal
    DUE_SOON,    // Vence pronto (próximos 7 días)
    DUE_TOMORROW,// Vence mañana
    DUE_TODAY,   // Vence hoy
    OVERDUE      // Vencida
}
