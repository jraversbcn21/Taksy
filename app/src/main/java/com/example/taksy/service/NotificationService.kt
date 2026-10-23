package com.example.taksy.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.taksy.MainActivity
import com.example.taksy.R
import com.example.taksy.data.Reminder
import com.example.taksy.data.Task
import com.example.taksy.receiver.ReminderActionReceiver

/**
 * Servicio para manejar notificaciones de recordatorios
 */
class NotificationService(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "taksy_reminders"
        private var notificationId = 1000
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 300, 200, 300)
            setShowBadge(true)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    // Un request code por recordatorio y acción: con el mismo, los tres botones compartirían PendingIntent
    private fun actionIntent(action: String, reminder: Reminder, task: Task): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java).apply {
            this.action = action
            putExtra(ReminderActionReceiver.EXTRA_TASK_ID, task.id)
            putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, reminder.id.toInt())
        }
        return PendingIntent.getBroadcast(
            context,
            reminder.id.toInt() * 3 + when (action) {
                ReminderActionReceiver.ACTION_DONE -> 0
                ReminderActionReceiver.ACTION_SNOOZE_1H -> 1
                else -> 2
            },
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun showReminderNotification(reminder: Reminder, task: Task) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("task_id", task.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Título y descripción de la tarea actual: reminder.titulo es una copia que no se
        // actualiza al renombrar la tarea
        val contentText = task.descripcion?.takeIf { it.isNotBlank() }
            ?: reminder.descripcion?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.notification_default_content, task.titulo)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ticksy_icon)
            .setContentTitle(task.titulo)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setLights(0xFF4CAF50.toInt(), 1000, 1000)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(0, context.getString(R.string.notification_action_done), actionIntent(ReminderActionReceiver.ACTION_DONE, reminder, task))
            .addAction(0, context.getString(R.string.notification_action_snooze_1h), actionIntent(ReminderActionReceiver.ACTION_SNOOZE_1H, reminder, task))
            .addAction(0, context.getString(R.string.notification_action_snooze_tomorrow), actionIntent(ReminderActionReceiver.ACTION_SNOOZE_TOMORROW, reminder, task))
            .build()

        // Sin el permiso (Android 13+) la notificación no se puede mostrar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        NotificationManagerCompat.from(context).notify(reminder.id.toInt(), notification)
    }
}
