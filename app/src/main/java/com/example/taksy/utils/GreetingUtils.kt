package com.example.taksy.utils

import androidx.annotation.StringRes
import com.example.taksy.R

enum class DayPeriod { MORNING, AFTERNOON, NIGHT }

/**
 * Saludo de la pantalla de inicio según la hora local.
 * Mañana 06:00–11:59 · Tarde 12:00–19:59 · Noche 20:00–05:59
 */
object GreetingUtils {

    fun periodFor(hourOfDay: Int): DayPeriod = when (hourOfDay) {
        in 6..11 -> DayPeriod.MORNING
        in 12..19 -> DayPeriod.AFTERNOON
        else -> DayPeriod.NIGHT
    }

    @StringRes
    fun greetingRes(hourOfDay: Int): Int = when (periodFor(hourOfDay)) {
        DayPeriod.MORNING -> R.string.greeting_morning
        DayPeriod.AFTERNOON -> R.string.greeting_afternoon
        DayPeriod.NIGHT -> R.string.greeting_night
    }
}
