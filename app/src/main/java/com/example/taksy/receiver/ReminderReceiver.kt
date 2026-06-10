package com.example.taksy.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.taksy.data.AppDatabase
import com.example.taksy.data.Reminder
import com.example.taksy.data.Task
import com.example.taksy.service.NotificationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver para manejar recordatorios programados y reprogramarlos tras reinicio.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // goAsync() keeps the process alive while the coroutine runs.
        // Without it, Android may kill the process before DB query + notification completes.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    Intent.ACTION_BOOT_COMPLETED -> {
                        rescheduleAllRemindersAfterBoot(context)
                        com.example.taksy.service.DailyReminderManager.rescheduleFromPrefs(context)
                    }
                    else -> handleReminderAlarm(context, intent)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Reprograma todos los recordatorios activos tras un reinicio del dispositivo.
     * AlarmManager no persiste alarmas entre reinicios.
     */
    private suspend fun rescheduleAllRemindersAfterBoot(context: Context) {
        val database = AppDatabase.getDatabase(context)
        val activeReminders = database.reminderDao().getAllActiveRemindersSync()
        val scheduler = com.example.taksy.service.ReminderScheduler(context)
        activeReminders.forEach { reminder ->
            if (reminder.fechaRecordatorio.time > System.currentTimeMillis()) {
                scheduler.scheduleReminder(reminder)
            }
        }
    }

    private suspend fun handleReminderAlarm(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra("reminder_id", -1)
        if (reminderId == -1L) return

        val database = AppDatabase.getDatabase(context)
        val reminder = database.reminderDao().getReminderById(reminderId)
        val task = database.taskDao().getTaskById(reminder?.taskId ?: -1)

        if (reminder != null && task != null) {
            val notificationService = NotificationService(context)
            notificationService.showReminderNotification(reminder, task)

            // Si es un recordatorio recurrente, programar el siguiente
            if (reminder.tipoRecordatorio != com.example.taksy.data.TipoRecordatorio.UNA_VEZ) {
                scheduleNextRecurringReminder(context, reminder)
            }
        }
    }
    
    /**
     * Programa el siguiente recordatorio recurrente
     */
    private suspend fun scheduleNextRecurringReminder(context: Context, reminder: Reminder) {
        val database = AppDatabase.getDatabase(context)
        val task = database.taskDao().getTaskById(reminder.taskId)

        if (task?.estado != com.example.taksy.data.TaskEstado.COMPLETADA) {
            val nextDate = com.example.taksy.domain.RecurrenceCalculator.advance(
                reminder.fechaRecordatorio,
                reminder.tipoRecordatorio
            ) ?: return

            val updatedReminder = reminder.copy(fechaRecordatorio = nextDate)
            database.reminderDao().updateReminder(updatedReminder)
            com.example.taksy.service.ReminderScheduler(context).scheduleReminder(updatedReminder)
        }
    }
}
