package com.example.taksy.domain

import com.example.taksy.data.TaskRecurrencia
import com.example.taksy.data.TipoRecordatorio
import java.util.Calendar
import java.util.Date

object RecurrenceCalculator {

    fun advance(from: Date, recurrencia: TaskRecurrencia): Date {
        val cal = Calendar.getInstance().apply { time = from }
        when (recurrencia) {
            TaskRecurrencia.DIARIA -> cal.add(Calendar.DAY_OF_MONTH, 1)
            TaskRecurrencia.SEMANAL -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            TaskRecurrencia.MENSUAL -> cal.add(Calendar.MONTH, 1)
            TaskRecurrencia.ANUAL -> cal.add(Calendar.YEAR, 1)
            TaskRecurrencia.NINGUNA -> {}
        }
        return cal.time
    }

    fun advance(from: Date, tipo: TipoRecordatorio): Date? {
        val cal = Calendar.getInstance().apply { time = from }
        when (tipo) {
            TipoRecordatorio.DIARIO -> cal.add(Calendar.DAY_OF_MONTH, 1)
            TipoRecordatorio.SEMANAL -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            TipoRecordatorio.MENSUAL -> cal.add(Calendar.MONTH, 1)
            TipoRecordatorio.UNA_VEZ -> return null
        }
        return cal.time
    }
}
