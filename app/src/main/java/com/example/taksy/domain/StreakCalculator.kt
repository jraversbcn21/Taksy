package com.example.taksy.domain

import java.util.Calendar
import java.util.Date

/** Racha: días seguidos con alguna tarea completada. */
object StreakCalculator {

    /**
     * Si hoy aún no hay ninguna completada la racha sigue viva desde ayer. Los días se cuentan con
     * el calendario y no restando 24 h, que falla el día del cambio de hora (23 o 25 h).
     */
    fun compute(completedAt: List<Date>, now: Date = Date()): Int {
        if (completedAt.isEmpty()) return 0
        val days = completedAt.map { startOfDay(it) }.toSet()
        val cursor = Calendar.getInstance().apply { time = startOfDay(now) }
        if (cursor.time !in days) cursor.add(Calendar.DAY_OF_MONTH, -1)
        var streak = 0
        while (cursor.time in days) {
            streak++
            cursor.add(Calendar.DAY_OF_MONTH, -1)
        }
        return streak
    }

    private fun startOfDay(date: Date): Date = Calendar.getInstance().apply {
        time = date
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.time
}
