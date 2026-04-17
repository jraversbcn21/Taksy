package com.example.taksy.viewmodel

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import com.example.taksy.services.DailyReminderService
import java.util.*

/**
 * ViewModel para manejar recordatorios diarios
 */
class ReminderViewModel : ViewModel() {
    
    companion object {
        private const val REQUEST_CODE_MORNING = 1001
        private const val REQUEST_CODE_EVENING = 1002
    }
    
    private var alarmManager: AlarmManager? = null
    
    fun initializeAlarmManager(context: Context) {
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }
    
    /**
     * Programa recordatorios diarios
     */
    fun scheduleDailyReminders(
        context: Context,
        morningHour: Int,
        morningMinute: Int,
        eveningHour: Int,
        eveningMinute: Int,
        enabled: Boolean
    ) {
        android.util.Log.d("ReminderViewModel", "=== PROGRAMANDO RECORDATORIOS ===")
        android.util.Log.d("ReminderViewModel", "Habilitado: $enabled")
        android.util.Log.d("ReminderViewModel", "Hora matutina: $morningHour:$morningMinute")
        android.util.Log.d("ReminderViewModel", "Hora vespertina: $eveningHour:$eveningMinute")
        
        if (alarmManager == null) {
            initializeAlarmManager(context)
        }
        
        // Cancelar recordatorios existentes
        android.util.Log.d("ReminderViewModel", "Cancelando recordatorios existentes...")
        cancelDailyReminders(context)
        
        if (!enabled) {
            android.util.Log.d("ReminderViewModel", "Recordatorios deshabilitados, no se programan")
            return
        }
        
        // Crear canal de notificaciones
        android.util.Log.d("ReminderViewModel", "Creando canal de notificaciones...")
        DailyReminderService.createNotificationChannel(context)
        
        val calendar = Calendar.getInstance()
        
        // Programar recordatorio matutino
        android.util.Log.d("ReminderViewModel", "Programando recordatorio matutino...")
        val morningIntent = Intent(context, DailyReminderService::class.java).apply {
            putExtra("reminder_type", "morning")
        }
        val morningPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_MORNING,
            morningIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        calendar.set(Calendar.HOUR_OF_DAY, morningHour)
        calendar.set(Calendar.MINUTE, morningMinute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val currentTime = System.currentTimeMillis()
        val triggerTime = calendar.timeInMillis
        
        android.util.Log.d("ReminderViewModel", "Tiempo actual: ${java.util.Date(currentTime)}")
        android.util.Log.d("ReminderViewModel", "Tiempo programado: ${java.util.Date(triggerTime)}")
        
        // Si la hora ya pasó hoy, programar para mañana
        if (triggerTime <= currentTime) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            android.util.Log.d("ReminderViewModel", "Hora ya pasó hoy, programando para mañana: ${java.util.Date(calendar.timeInMillis)}")
        }
        
        try {
            alarmManager?.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                morningPendingIntent
            )
            android.util.Log.d("ReminderViewModel", "Recordatorio matutino programado exitosamente")
        } catch (e: Exception) {
            android.util.Log.e("ReminderViewModel", "Error programando recordatorio matutino: ${e.message}")
        }
        
        // Programar recordatorio vespertino
        android.util.Log.d("ReminderViewModel", "Programando recordatorio vespertino...")
        val eveningIntent = Intent(context, DailyReminderService::class.java).apply {
            putExtra("reminder_type", "evening")
        }
        val eveningPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_EVENING,
            eveningIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        calendar.set(Calendar.HOUR_OF_DAY, eveningHour)
        calendar.set(Calendar.MINUTE, eveningMinute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val eveningTriggerTime = calendar.timeInMillis
        android.util.Log.d("ReminderViewModel", "Tiempo programado vespertino: ${java.util.Date(eveningTriggerTime)}")
        
        // Si la hora ya pasó hoy, programar para mañana
        if (eveningTriggerTime <= currentTime) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            android.util.Log.d("ReminderViewModel", "Hora vespertina ya pasó hoy, programando para mañana: ${java.util.Date(calendar.timeInMillis)}")
        }
        
        try {
            alarmManager?.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                eveningPendingIntent
            )
            android.util.Log.d("ReminderViewModel", "Recordatorio vespertino programado exitosamente")
        } catch (e: Exception) {
            android.util.Log.e("ReminderViewModel", "Error programando recordatorio vespertino: ${e.message}")
        }
        
        android.util.Log.d("ReminderViewModel", "=== RECORDATORIOS PROGRAMADOS COMPLETAMENTE ===")
    }
    
    /**
     * Cancela todos los recordatorios diarios
     */
    fun cancelDailyReminders(context: Context) {
        if (alarmManager == null) {
            initializeAlarmManager(context)
        }
        
        val morningIntent = Intent(context, DailyReminderService::class.java)
        val morningPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_MORNING,
            morningIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val eveningIntent = Intent(context, DailyReminderService::class.java)
        val eveningPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_EVENING,
            eveningIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager?.cancel(morningPendingIntent)
        alarmManager?.cancel(eveningPendingIntent)
    }
    
    /**
     * Verifica si los recordatorios están programados
     */
    fun areRemindersScheduled(context: Context): Boolean {
        val morningIntent = Intent(context, DailyReminderService::class.java)
        val morningPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_MORNING,
            morningIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        
        return morningPendingIntent != null
    }
}
