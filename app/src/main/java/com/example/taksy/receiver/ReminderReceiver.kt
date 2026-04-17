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
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> rescheduleAllRemindersAfterBoot(context)
            else -> handleReminderAlarm(context, intent)
        }
    }

    /**
     * Reprograma todos los recordatorios activos tras un reinicio del dispositivo.
     * AlarmManager no persiste alarmas entre reinicios.
     */
    private fun rescheduleAllRemindersAfterBoot(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.getDatabase(context)
            val activeReminders = database.reminderDao().getAllActiveRemindersSync()
            val scheduler = com.example.taksy.service.ReminderScheduler(context)
            activeReminders.forEach { reminder ->
                // Solo reprogramar si la fecha es futura
                if (reminder.fechaRecordatorio.time > System.currentTimeMillis()) {
                    scheduler.scheduleReminder(reminder)
                }
            }
        }
    }

    private fun handleReminderAlarm(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra("reminder_id", -1)

        if (reminderId != -1L) {
            CoroutineScope(Dispatchers.IO).launch {
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
        }
    }
    
    /**
     * Programa el siguiente recordatorio recurrente
     */
    private fun scheduleNextRecurringReminder(context: Context, reminder: Reminder) {
        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.getDatabase(context)
            val task = database.taskDao().getTaskById(reminder.taskId)
            
            // Solo programar el siguiente recordatorio si la tarea no está completada
            if (task?.estado != com.example.taksy.data.TaskEstado.COMPLETADA) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                val intent = Intent(context, ReminderReceiver::class.java).apply {
                    putExtra("reminder_id", reminder.id)
                }
                
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    reminder.id.toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                val calendar = java.util.Calendar.getInstance()
                calendar.time = reminder.fechaRecordatorio
                
                val nextTime = when (reminder.tipoRecordatorio) {
                    com.example.taksy.data.TipoRecordatorio.DIARIO -> {
                        calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
                        calendar.timeInMillis
                    }
                    com.example.taksy.data.TipoRecordatorio.SEMANAL -> {
                        calendar.add(java.util.Calendar.WEEK_OF_YEAR, 1)
                        calendar.timeInMillis
                    }
                    com.example.taksy.data.TipoRecordatorio.MENSUAL -> {
                        calendar.add(java.util.Calendar.MONTH, 1)
                        calendar.timeInMillis
                    }
                    else -> return@launch
                }
                
                alarmManager.setExactAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    nextTime,
                    pendingIntent
                )
            }
        }
    }
}
