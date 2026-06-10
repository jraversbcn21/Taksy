package com.example.taksy.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.taksy.data.AppDatabase
import com.example.taksy.data.Reminder
import com.example.taksy.service.DailyReminderManager
import com.example.taksy.service.NotificationService
import com.example.taksy.service.ReminderSchedulerContract
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduler: ReminderSchedulerContract
    @Inject lateinit var database: AppDatabase

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    Intent.ACTION_BOOT_COMPLETED -> {
                        rescheduleAllRemindersAfterBoot()
                        DailyReminderManager.rescheduleFromPrefs(context)
                    }
                    else -> handleReminderAlarm(context, intent)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun rescheduleAllRemindersAfterBoot() {
        val activeReminders = database.reminderDao().getAllActiveRemindersSync()
        activeReminders.forEach { reminder ->
            if (reminder.fechaRecordatorio.time > System.currentTimeMillis()) {
                scheduler.scheduleReminder(reminder)
            }
        }
    }

    private suspend fun handleReminderAlarm(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra("reminder_id", -1)
        if (reminderId == -1L) return

        val reminder = database.reminderDao().getReminderById(reminderId)
        val task = database.taskDao().getTaskById(reminder?.taskId ?: -1)

        if (reminder != null && task != null) {
            NotificationService(context).showReminderNotification(reminder, task)

            if (reminder.tipoRecordatorio != com.example.taksy.data.TipoRecordatorio.UNA_VEZ) {
                scheduleNextRecurringReminder(reminder)
            }
        }
    }

    private suspend fun scheduleNextRecurringReminder(reminder: Reminder) {
        val task = database.taskDao().getTaskById(reminder.taskId)

        if (task?.estado != com.example.taksy.data.TaskEstado.COMPLETADA) {
            val nextDate = com.example.taksy.domain.RecurrenceCalculator.advance(
                reminder.fechaRecordatorio,
                reminder.tipoRecordatorio
            ) ?: return

            val updatedReminder = reminder.copy(fechaRecordatorio = nextDate)
            database.reminderDao().updateReminder(updatedReminder)
            scheduler.scheduleReminder(updatedReminder)
        }
    }
}
