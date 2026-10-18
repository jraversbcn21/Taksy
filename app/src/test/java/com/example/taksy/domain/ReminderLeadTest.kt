package com.example.taksy.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date

class ReminderLeadTest {

    private fun at(hour: Int, minute: Int): Date =
        Calendar.getInstance().apply { clear(); set(2026, Calendar.OCTOBER, 20, hour, minute) }.time

    @Test
    fun `00 00 is all day and gets the all-day options`() {
        assertTrue(ReminderLead.isAllDay(at(0, 0)))
        assertEquals(listOf(ReminderLead.SAME_DAY_9, ReminderLead.DAY_BEFORE_9), ReminderLead.optionsFor(at(0, 0)))
    }

    @Test
    fun `a time of day gets the timed options`() {
        assertFalse(ReminderLead.isAllDay(at(17, 30)))
        assertEquals(
            listOf(ReminderLead.AT_TIME, ReminderLead.MINUTES_10, ReminderLead.HOUR_1, ReminderLead.DAY_1),
            ReminderLead.optionsFor(at(17, 30))
        )
    }

    @Test
    fun `all-day leads land at 9 00 that day or the day before`() {
        assertEquals(at(9, 0), ReminderLead.SAME_DAY_9.reminderFor(at(0, 0)))
        val dayBefore = Calendar.getInstance().apply { time = at(9, 0); add(Calendar.DAY_OF_MONTH, -1) }.time
        assertEquals(dayBefore, ReminderLead.DAY_BEFORE_9.reminderFor(at(0, 0)))
    }

    @Test
    fun `match finds the option of an existing reminder`() {
        assertEquals(ReminderLead.MINUTES_10, ReminderLead.match(at(17, 30), at(17, 20)))
        assertEquals(ReminderLead.AT_TIME, ReminderLead.match(at(17, 30), at(17, 30)))
    }

    @Test
    fun `an offset that is not an option is custom`() {
        // Como "pope": vence a las 17:20 y avisa a las 17:25
        assertNull(ReminderLead.match(at(17, 20), at(17, 25)))
    }
}
