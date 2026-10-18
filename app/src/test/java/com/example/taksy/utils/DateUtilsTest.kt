package com.example.taksy.utils

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class DateUtilsTest {

    private lateinit var originalZone: TimeZone

    @Before
    fun setUp() {
        originalZone = TimeZone.getDefault()
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalZone)
    }

    private fun local(year: Int, month: Int, day: Int, hour: Int, minute: Int) =
        Calendar.getInstance().apply { clear(); set(year, month, day, hour, minute) }.time

    private fun utcMidnight(year: Int, month: Int, day: Int) =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { clear(); set(year, month, day) }.timeInMillis

    @Test
    fun `toUtcDay keeps the local day just after midnight east of UTC`() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Madrid"))
        // 00:30 en Madrid es todavía el día anterior en UTC
        val date = local(2026, Calendar.OCTOBER, 18, 0, 30)
        assertEquals(utcMidnight(2026, Calendar.OCTOBER, 18), DateUtils.toUtcDay(date))
    }

    @Test
    fun `toUtcDay keeps the local day late in the evening west of UTC`() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
        // 23:30 en Nueva York ya es el día siguiente en UTC
        val date = local(2026, Calendar.OCTOBER, 18, 23, 30)
        assertEquals(utcMidnight(2026, Calendar.OCTOBER, 18), DateUtils.toUtcDay(date))
    }

    @Test
    fun `combineUtcDay puts the local time on the picked day`() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
        val combined = DateUtils.combineUtcDay(utcMidnight(2026, Calendar.OCTOBER, 18), 9, 15)
        assertEquals(local(2026, Calendar.OCTOBER, 18, 9, 15), combined)
    }

    @Test
    fun `round trip keeps day and time`() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Madrid"))
        val original = local(2026, Calendar.MARCH, 29, 0, 30) // día del cambio de hora
        val roundTrip = DateUtils.combineUtcDay(DateUtils.toUtcDay(original), 0, 30)
        assertEquals(original, roundTrip)
    }
}
