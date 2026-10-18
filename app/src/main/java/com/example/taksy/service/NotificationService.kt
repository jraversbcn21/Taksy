package com.example.taksy.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.taksy.MainActivity
import com.example.taksy.R
import com.example.taksy.data.Reminder
import com.example.taksy.data.Task

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
            .build()

        with(NotificationManagerCompat.from(context)) {
            notify(reminder.id.toInt(), notification)
        }
    }
    
    /**
     * Cancela una notificación específica
     */
    fun cancelNotification(reminderId: Long) {
        with(NotificationManagerCompat.from(context)) {
            cancel(reminderId.toInt())
        }
    }
    
    /**
     * Cancela todas las notificaciones
     */
    fun cancelAllNotifications() {
        with(NotificationManagerCompat.from(context)) {
            cancelAll()
        }
    }
}
