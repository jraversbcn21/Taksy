package com.example.taksy.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.taksy.data.preferences.PreferencesRepository
import java.util.Calendar

object DailyReminderManager {

    private const val REQUEST_CODE_MORNING = 1001
    private const val REQUEST_CODE_EVENING = 1002

    fun loadPrefs(context: Context): PreferencesRepository.DailyReminderPrefs =
        PreferencesRepository(context.applicationContext).loadDailyReminderPrefs()

    fun scheduleDailyReminders(
        context: Context,
        morningHour: Int,
        morningMinute: Int,
        eveningHour: Int,
        eveningMinute: Int,
        enabled: Boolean
    ) {
        PreferencesRepository(context.applicationContext).saveDailyReminderPrefs(
            PreferencesRepository.DailyReminderPrefs(
                enabled = enabled,
                morningHour = morningHour,
                morningMinute = morningMinute,
                eveningHour = eveningHour,
                eveningMinute = eveningMinute
            )
        )

        cancelDailyReminders(context)
        if (!enabled) return

        DailyReminderService.createNotificationChannel(context)
        scheduleExactAlarm(context, morningHour, morningMinute, REQUEST_CODE_MORNING, "morning")
        scheduleExactAlarm(context, eveningHour, eveningMinute, REQUEST_CODE_EVENING, "evening")
    }

    fun cancelDailyReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        listOf(REQUEST_CODE_MORNING, REQUEST_CODE_EVENING).forEach { requestCode ->
            val intent = Intent(context, DailyReminderService::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

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
            val canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                alarmManager.canScheduleExactAlarms()
            if (canScheduleExact) {
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
        } catch (_: Exception) {
        }
    }

    fun rescheduleFromPrefs(context: Context) {
        val prefs = loadPrefs(context)
        if (!prefs.enabled) return

        DailyReminderService.createNotificationChannel(context)
        scheduleExactAlarm(context, prefs.morningHour, prefs.morningMinute, REQUEST_CODE_MORNING, "morning")
        scheduleExactAlarm(context, prefs.eveningHour, prefs.eveningMinute, REQUEST_CODE_EVENING, "evening")
    }
}
