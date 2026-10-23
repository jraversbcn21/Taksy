package com.example.taksy.domain

import java.util.Calendar
import java.util.Date

/** Tareas completadas por día, para el gráfico de Estadísticas. */
object DailyCompletions {

    /**
     * Los [days] últimos días terminando hoy (el más antiguo primero), cada uno con su inicio y
     * cuántas tareas se completaron ese día. Los días se recorren con el calendario y no con
     * saltos de 24 h, que se desplazan una hora el día del cambio de hora (23 o 25 h).
     */
    fun lastDays(completedAt: List<Date>, days: Int = 7, now: Date = Date()): List<Pair<Date, Int>> {
        val today = Calendar.getInstance().apply {
            time = now
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return (days - 1 downTo 0).map { back ->
            val start = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -back) }
            val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
            start.time to completedAt.count { !it.before(start.time) && it.before(end.time) }
        }
    }
}
