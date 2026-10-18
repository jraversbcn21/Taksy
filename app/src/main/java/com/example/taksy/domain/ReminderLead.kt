package com.example.taksy.domain

import java.util.Calendar
import java.util.Date

/**
 * El aviso de una tarea se expresa respecto a su fecha: recordatorio = vencimiento + [offsetMinutes].
 * Una tarea "sin hora" se guarda a las 00:00, así que tiene sus propias opciones (a las 9:00
 * de ese día o del anterior) en vez de "a la hora", que avisaría a medianoche.
 */
enum class ReminderLead(val offsetMinutes: Long, val allDay: Boolean) {
    AT_TIME(0, allDay = false),
    MINUTES_10(-10, allDay = false),
    HOUR_1(-60, allDay = false),
    DAY_1(-24 * 60, allDay = false),
    SAME_DAY_9(9 * 60, allDay = true),
    DAY_BEFORE_9(-15 * 60, allDay = true);

    fun reminderFor(due: Date): Date = Date(due.time + offsetMinutes * 60_000)

    companion object {
        /** Sin hora = las 00:00 exactas, igual que en el resto de la app. */
        fun isAllDay(due: Date): Boolean {
            val cal = Calendar.getInstance().apply { time = due }
            return cal.get(Calendar.HOUR_OF_DAY) == 0 && cal.get(Calendar.MINUTE) == 0
        }

        fun optionsFor(due: Date): List<ReminderLead> = entries.filter { it.allDay == isAllDay(due) }

        /** Opción que corresponde a un recordatorio existente, o null si es una hora personalizada. */
        fun match(due: Date, reminder: Date): ReminderLead? {
            val offset = (reminder.time - due.time) / 60_000
            return optionsFor(due).firstOrNull { it.offsetMinutes == offset }
        }
    }
}
