package com.example.taksy.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.taksy.data.Reminder
import com.example.taksy.receiver.ReminderReceiver

class ReminderScheduler(private val context: Context) : ReminderSchedulerContract {
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    override fun scheduleReminder(reminder: Reminder) {
        android.util.Log.d("ReminderScheduler", "=== Programando recordatorio ===")
        android.util.Log.d("ReminderScheduler", "ID: ${reminder.id}")
        android.util.Log.d("ReminderScheduler", "Título: ${reminder.titulo}")
        android.util.Log.d("ReminderScheduler", "Fecha: ${reminder.fechaRecordatorio}")
        android.util.Log.d("ReminderScheduler", "Tipo: ${reminder.tipoRecordatorio}")
        android.util.Log.d("ReminderScheduler", "Activo: ${reminder.activo}")
        
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("reminder_id", reminder.id)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val triggerTime = reminder.fechaRecordatorio.time
        val currentTime = System.currentTimeMillis()
        val timeUntilTrigger = triggerTime - currentTime
        
        android.util.Log.d("ReminderScheduler", "Tiempo actual: $currentTime (${java.util.Date(currentTime)})")
        android.util.Log.d("ReminderScheduler", "Tiempo de activación: $triggerTime (${java.util.Date(triggerTime)})")
        android.util.Log.d("ReminderScheduler", "Tiempo hasta activación: ${timeUntilTrigger}ms (${timeUntilTrigger / 1000}s)")
        
        // Verificar si la fecha es en el futuro
        if (timeUntilTrigger <= 0) {
            android.util.Log.w("ReminderScheduler", "⚠️ ADVERTENCIA: La fecha del recordatorio es en el pasado o muy pronto")
            android.util.Log.w("ReminderScheduler", "Diferencia: ${timeUntilTrigger}ms")
        }
        
        // Schedule the alarm, with fallback for missing exact alarm permission (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            android.util.Log.w("ReminderScheduler", "No exact alarm permission, using inexact alarm")
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }

        android.util.Log.d("ReminderScheduler", "Alarma programada exitosamente")
    }
    
    override fun cancelReminder(reminderId: Long) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
    }
    
    override fun cancelAllReminders() {
    }
}
