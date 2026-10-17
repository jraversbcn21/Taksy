package com.example.taksy.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class GreetingUtilsTest {

    @Test
    fun `boundaries map to the right period`() {
        assertEquals(DayPeriod.NIGHT, GreetingUtils.periodFor(0))
        assertEquals(DayPeriod.NIGHT, GreetingUtils.periodFor(5))
        assertEquals(DayPeriod.MORNING, GreetingUtils.periodFor(6))
        assertEquals(DayPeriod.MORNING, GreetingUtils.periodFor(11))
        assertEquals(DayPeriod.AFTERNOON, GreetingUtils.periodFor(12))
        assertEquals(DayPeriod.AFTERNOON, GreetingUtils.periodFor(19))
        assertEquals(DayPeriod.NIGHT, GreetingUtils.periodFor(20))
        assertEquals(DayPeriod.NIGHT, GreetingUtils.periodFor(23))
    }
}
