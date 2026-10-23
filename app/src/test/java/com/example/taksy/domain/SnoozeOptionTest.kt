package com.example.taksy.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.Date

class SnoozeOptionTest {

    private fun at(day: Int, hour: Int, minute: Int): Date =
        Calendar.getInstance().apply { clear(); set(2026, Calendar.OCTOBER, day, hour, minute) }.time

    @Test
    fun `one hour snooze adds 60 minutes`() {
        assertEquals(at(23, 15, 45), SnoozeOption.HOUR_1.from(at(23, 14, 45)))
    }

    @Test
    fun `tomorrow snooze lands at 9 00 the next day`() {
        assertEquals(at(24, 9, 0), SnoozeOption.TOMORROW.from(at(23, 14, 45)))
    }

    @Test
    fun `tomorrow snooze rolls over the end of the month`() {
        val now = Calendar.getInstance().apply { clear(); set(2026, Calendar.OCTOBER, 31, 22, 0) }.time
        val expected = Calendar.getInstance().apply { clear(); set(2026, Calendar.NOVEMBER, 1, 9, 0) }.time
        assertEquals(expected, SnoozeOption.TOMORROW.from(now))
    }
}
