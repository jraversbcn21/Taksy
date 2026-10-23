package com.example.taksy.domain

import java.util.Calendar
import java.util.Date

/** Botones "Posponer" de la notificación de un recordatorio. */
enum class SnoozeOption {
    HOUR_1,
    TOMORROW;

    /** Nueva hora del aviso: dentro de una hora, o mañana a las 9:00. */
    fun from(now: Date): Date = when (this) {
        HOUR_1 -> Date(now.time + 60 * 60_000L)
        TOMORROW -> Calendar.getInstance().apply {
            time = now
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }
}
