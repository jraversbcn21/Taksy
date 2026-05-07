package com.example.taksy.viewmodel

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import com.example.taksy.service.DailyReminderService
import java.util.*

/**
 * ViewModel para manejar recordatorios diarios
 */
class ReminderViewModel : ViewModel() {

    companion object {
        private const val REQUEST_CODE_MORNING = 1001
        private const val REQUEST_CODE_EVENING = 1002
        private const val PREFS_NAME = "daily_reminder_prefs"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_MORNING_HOUR = "morning_hour"
        private const val KEY_MORNING_MINUTE = "morning_minute"
        private const val KEY_EVENING_HOUR = "evening_hour"
        private const val KEY_EVENING_MINUTE = "evening_minute"

        /**
         * Carga las preferencias guardadas de recordatorios diarios.
         */
        fun loadPrefs(context: Context): DailyReminderPrefs {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return DailyReminderPrefs(
                enabled = prefs.getBoolean(KEY_ENABLED, false),
                morningHour = prefs.getInt(KEY_MORNING_HOUR, 10),
                morningMinute = prefs.getInt(KEY_MORNING_MINUTE, 0),
                eveningHour = prefs.getInt(KEY_EVENING_HOUR, 18),
                eveningMinute = prefs.getInt(KEY_EVENING_MINUTE, 0)
            )
        }

        /**
         * Programa una alarma exacta para recordatorios diarios.
         * Se usa desde el ViewModel y desde el BroadcastReceiver para reprogramar.
         */
        fun scheduleExactAlarm(
            context: Context,
            hour: Int,
            minute: Int,
            requestCode: Int,
            reminderType: String
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(context, DailyReminderService::class.java).apply {
                putExtra("reminder_type", reminderType)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
                android.util.Log.d("ReminderViewModel", "Alarma $reminderType programada para: ${Date(calendar.timeInMillis)}")
            } catch (e: Exception) {
                android.util.Log.e("ReminderViewModel", "Error programando alarma $reminderType: ${e.message}")
            }
        }

        /**
         * Reprograma ambos recordatorios diarios desde las preferencias guardadas.
         * Usado tras reinicio del dispositivo.
         */
        fun rescheduleFromPrefs(context: Context) {
            val prefs = loadPrefs(context)
            if (!prefs.enabled) return

            DailyReminderService.createNotificationChannel(context)
            scheduleExactAlarm(context, prefs.morningHour, prefs.morningMinute, REQUEST_CODE_MORNING, "morning")
            scheduleExactAlarm(context, prefs.eveningHour, prefs.eveningMinute, REQUEST_CODE_EVENING, "evening")
            android.util.Log.d("ReminderViewModel", "Recordatorios diarios reprogramados desde preferencias")
        }
    }

    data class DailyReminderPrefs(
        val enabled: Boolean,
        val morningHour: Int,
        val morningMinute: Int,
        val eveningHour: Int,
        val eveningMinute: Int
    )

    private var alarmManager: AlarmManager? = null

    fun initializeAlarmManager(context: Context) {
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    /**
     * Programa recordatorios diarios y persiste la configuración.
     */
    fun scheduleDailyReminders(
        context: Context,
        morningHour: Int,
        morningMinute: Int,
        eveningHour: Int,
        eveningMinute: Int,
        enabled: Boolean
    ) {
        // Persistir configuración
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putBoolean(KEY_ENABLED, enabled)
            putInt(KEY_MORNING_HOUR, morningHour)
            putInt(KEY_MORNING_MINUTE, morningMinute)
            putInt(KEY_EVENING_HOUR, eveningHour)
            putInt(KEY_EVENING_MINUTE, eveningMinute)
            apply()
        }

        // Cancelar recordatorios existentes
        cancelDailyReminders(context)

        if (!enabled) return

        // Crear canal de notificaciones
        DailyReminderService.createNotificationChannel(context)

        // Programar alarmas exactas
        scheduleExactAlarm(context, morningHour, morningMinute, REQUEST_CODE_MORNING, "morning")
        scheduleExactAlarm(context, eveningHour, eveningMinute, REQUEST_CODE_EVENING, "evening")

        android.util.Log.d("ReminderViewModel", "Recordatorios diarios programados: matutino $morningHour:$morningMinute, vespertino $eveningHour:$eveningMinute")
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
}
