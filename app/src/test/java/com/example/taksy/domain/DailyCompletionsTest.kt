package com.example.taksy.domain

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class DailyCompletionsTest {

    private lateinit var originalZone: TimeZone

    // El cambio de hora de otoño en España: el 25-oct-2026 a las 03:00 se vuelve a las 02:00
    @Before
    fun useMadrid() {
        originalZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Madrid"))
    }

    @After
    fun restoreZone() = TimeZone.setDefault(originalZone)

    private fun at(month: Int, day: Int, hour: Int = 12, minute: Int = 0): Date =
        Calendar.getInstance().apply { clear(); set(2026, month, day, hour, minute) }.time

    @Test
    fun `returns the requested number of days, oldest first, ending today`() {
        val result = DailyCompletions.lastDays(emptyList(), 7, at(Calendar.OCTOBER, 23))
        assertEquals(7, result.size)
        assertEquals(at(Calendar.OCTOBER, 17, 0), result.first().first)
        assertEquals(at(Calendar.OCTOBER, 23, 0), result.last().first)
    }

    @Test
    fun `counts completions in their own day`() {
        val done = listOf(at(Calendar.OCTOBER, 23, 9), at(Calendar.OCTOBER, 23, 20), at(Calendar.OCTOBER, 21))
        val counts = DailyCompletions.lastDays(done, 7, at(Calendar.OCTOBER, 23, 21)).map { it.second }
        assertEquals(listOf(0, 0, 0, 0, 1, 0, 2), counts)
    }

    @Test
    fun `ignores completions older than the window`() {
        val counts = DailyCompletions.lastDays(listOf(at(Calendar.OCTOBER, 1)), 7, at(Calendar.OCTOBER, 23)).map { it.second }
        assertEquals(List(7) { 0 }, counts)
    }

    @Test
    fun `the first hour of the 25 hour day of the DST change stays in that day`() {
        // Con saltos de 24 h desde el 26 el día 25 empezaría a la 01:00, y lo hecho a las 00:30
        // caería en el día 24
        val done = listOf(at(Calendar.OCTOBER, 25, 0, 30))
        val result = DailyCompletions.lastDays(done, 7, at(Calendar.OCTOBER, 26, 18))
        val byDay = result.associate { Calendar.getInstance().apply { time = it.first }.get(Calendar.DAY_OF_MONTH) to it.second }
        assertEquals(1, byDay[25])
        assertEquals(0, byDay[24])
    }

    @Test
    fun `every bucket starts at midnight across the DST change`() {
        val result = DailyCompletions.lastDays(emptyList(), 7, at(Calendar.OCTOBER, 28))
        result.forEach {
            val cal = Calendar.getInstance().apply { time = it.first }
            assertEquals(0, cal.get(Calendar.HOUR_OF_DAY))
            assertEquals(0, cal.get(Calendar.MINUTE))
        }
    }
}
