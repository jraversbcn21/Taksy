package com.example.taksy.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.Date

class StreakCalculatorTest {

    private fun at(month: Int, day: Int, hour: Int = 12): Date =
        Calendar.getInstance().apply { clear(); set(2026, month, day, hour, 0) }.time

    private val oct23 = at(Calendar.OCTOBER, 23, 18)

    @Test
    fun `no completions is zero`() {
        assertEquals(0, StreakCalculator.compute(emptyList(), oct23))
    }

    @Test
    fun `consecutive days including today`() {
        val done = listOf(at(Calendar.OCTOBER, 23), at(Calendar.OCTOBER, 22), at(Calendar.OCTOBER, 21))
        assertEquals(3, StreakCalculator.compute(done, oct23))
    }

    @Test
    fun `streak survives a day with nothing done yet today`() {
        val done = listOf(at(Calendar.OCTOBER, 22), at(Calendar.OCTOBER, 21))
        assertEquals(2, StreakCalculator.compute(done, oct23))
    }

    @Test
    fun `a missed day breaks it`() {
        val done = listOf(at(Calendar.OCTOBER, 23), at(Calendar.OCTOBER, 21), at(Calendar.OCTOBER, 20))
        assertEquals(1, StreakCalculator.compute(done, oct23))
    }

    @Test
    fun `nothing today or yesterday is zero`() {
        assertEquals(0, StreakCalculator.compute(listOf(at(Calendar.OCTOBER, 20)), oct23))
    }

    @Test
    fun `several completions in a day count once`() {
        val done = listOf(at(Calendar.OCTOBER, 23, 9), at(Calendar.OCTOBER, 23, 20), at(Calendar.OCTOBER, 22))
        assertEquals(2, StreakCalculator.compute(done, oct23))
    }

    @Test
    fun `counts across the end of the month`() {
        val done = listOf(at(Calendar.NOVEMBER, 1), at(Calendar.OCTOBER, 31), at(Calendar.OCTOBER, 30))
        assertEquals(3, StreakCalculator.compute(done, at(Calendar.NOVEMBER, 1, 18)))
    }
}
