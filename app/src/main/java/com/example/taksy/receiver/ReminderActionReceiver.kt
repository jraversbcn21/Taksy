package com.example.taksy.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.taksy.domain.CompleteTaskUseCase
import com.example.taksy.domain.SnoozeOption
import com.example.taksy.domain.TaskReminderUseCase
import com.example.taksy.repository.TaskRepository
import com.example.taksy.widget.TaskWidgetProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Botones de la notificación de un recordatorio: Hecho y Posponer. */
@AndroidEntryPoint
class ReminderActionReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: TaskRepository
    @Inject lateinit var completeTask: CompleteTaskUseCase
    @Inject lateinit var reminderUseCase: TaskReminderUseCase

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val action = intent.action
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = repository.getTaskById(taskId) ?: return@launch
                when (action) {
                    ACTION_DONE -> completeTask.execute(task)
                    ACTION_SNOOZE_1H -> reminderUseCase.snooze(task, SnoozeOption.HOUR_1)
                    ACTION_SNOOZE_TOMORROW -> reminderUseCase.snooze(task, SnoozeOption.TOMORROW)
                    else -> return@launch
                }
                TaskWidgetProvider.refreshAll(context)
            } finally {
                // Al pulsar un botón la notificación se cierra, haya ido bien o no
                context.getSystemService(NotificationManager::class.java).cancel(notificationId)
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_DONE = "com.example.taksy.action.REMINDER_DONE"
        const val ACTION_SNOOZE_1H = "com.example.taksy.action.REMINDER_SNOOZE_1H"
        const val ACTION_SNOOZE_TOMORROW = "com.example.taksy.action.REMINDER_SNOOZE_TOMORROW"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
}
