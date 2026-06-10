package com.example.taksy.domain

import com.example.taksy.data.TaskRecurrencia
import com.example.taksy.data.TipoRecordatorio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.Date

class RecurrenceCalculatorTest {

    private fun dateOf(year: Int, month: Int, day: Int, hour: Int = 10, min: Int = 0): Date {
        val cal = Calendar.getInstance().apply {
            clear()
            set(year, month, day, hour, min, 0)
        }
        return cal.time
    }

    private fun fieldsOf(d: Date): Triple<Int, Int, Int> {
        val cal = Calendar.getInstance().apply { time = d }
        return Triple(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `task NINGUNA returns same date`() {
        val from = dateOf(2026, Calendar.JUNE, 10)
        val result = RecurrenceCalculator.advance(from, TaskRecurrencia.NINGUNA)
        assertEquals(from, result)
    }

    @Test
    fun `task DIARIA adds one day`() {
        val from = dateOf(2026, Calendar.JUNE, 10)
        val result = RecurrenceCalculator.advance(from, TaskRecurrencia.DIARIA)
        assertEquals(Triple(2026, Calendar.JUNE, 11), fieldsOf(result))
    }

    @Test
    fun `task SEMANAL adds one week`() {
        val from = dateOf(2026, Calendar.JUNE, 10)
        val result = RecurrenceCalculator.advance(from, TaskRecurrencia.SEMANAL)
        assertEquals(Triple(2026, Calendar.JUNE, 17), fieldsOf(result))
    }

    @Test
    fun `task MENSUAL adds one month`() {
        val from = dateOf(2026, Calendar.JUNE, 10)
        val result = RecurrenceCalculator.advance(from, TaskRecurrencia.MENSUAL)
        assertEquals(Triple(2026, Calendar.JULY, 10), fieldsOf(result))
    }

    @Test
    fun `task ANUAL adds one year`() {
        val from = dateOf(2026, Calendar.JUNE, 10)
        val result = RecurrenceCalculator.advance(from, TaskRecurrencia.ANUAL)
        assertEquals(Triple(2027, Calendar.JUNE, 10), fieldsOf(result))
    }

    @Test
    fun `task MENSUAL handles month-end overflow`() {
        val from = dateOf(2026, Calendar.JANUARY, 31)
        val result = RecurrenceCalculator.advance(from, TaskRecurrencia.MENSUAL)
        val (y, m, _) = fieldsOf(result)
        assertEquals(2026, y)
        assertEquals(Calendar.FEBRUARY, m)
    }

    @Test
    fun `reminder UNA_VEZ returns null`() {
        val from = dateOf(2026, Calendar.JUNE, 10)
        assertNull(RecurrenceCalculator.advance(from, TipoRecordatorio.UNA_VEZ))
    }

    @Test
    fun `reminder DIARIO adds one day`() {
        val from = dateOf(2026, Calendar.JUNE, 10)
        val result = RecurrenceCalculator.advance(from, TipoRecordatorio.DIARIO)!!
        assertEquals(Triple(2026, Calendar.JUNE, 11), fieldsOf(result))
    }

    @Test
    fun `reminder SEMANAL adds one week`() {
        val from = dateOf(2026, Calendar.JUNE, 10)
        val result = RecurrenceCalculator.advance(from, TipoRecordatorio.SEMANAL)!!
        assertEquals(Triple(2026, Calendar.JUNE, 17), fieldsOf(result))
    }

    @Test
    fun `reminder MENSUAL adds one month`() {
        val from = dateOf(2026, Calendar.JUNE, 10)
        val result = RecurrenceCalculator.advance(from, TipoRecordatorio.MENSUAL)!!
        assertEquals(Triple(2026, Calendar.JULY, 10), fieldsOf(result))
    }
}
