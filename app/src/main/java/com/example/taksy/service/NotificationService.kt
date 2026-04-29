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
        private const val CHANNEL_NAME = "Recordatorios de Taksy"
        private const val CHANNEL_DESCRIPTION = "Notificaciones para recordatorios de tareas"
        
        // IDs únicos para cada notificación
        private var notificationId = 1000
    }
    
    init {
        createNotificationChannel()
    }
    
    /**
     * Crea el canal de notificaciones
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                setShowBadge(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Muestra una notificación de recordatorio
     */
    fun showReminderNotification(reminder: Reminder, task: Task) {
        android.util.Log.d("NotificationService", "=== Creando notificación ===")
        android.util.Log.d("NotificationService", "Recordatorio: ${reminder.titulo}")
        android.util.Log.d("NotificationService", "Tarea: ${task.titulo}")
        
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
        
        val frequencyText = when (reminder.tipoRecordatorio) {
            com.example.taksy.data.TipoRecordatorio.UNA_VEZ -> "Recordatorio único"
            com.example.taksy.data.TipoRecordatorio.DIARIO -> "Recordatorio diario"
            com.example.taksy.data.TipoRecordatorio.SEMANAL -> "Recordatorio semanal"
            com.example.taksy.data.TipoRecordatorio.MENSUAL -> "Recordatorio mensual"
        }
        
        val contentText = if (reminder.descripcion.isNullOrBlank()) {
            "No olvides completar: ${task.titulo}"
        } else {
            reminder.descripcion
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ticksy_icon)
            .setContentTitle(reminder.titulo)
            .setContentText("Tarea: ${task.titulo}")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$contentText\n\n$frequencyText"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setLights(0xFF4CAF50.toInt(), 1000, 1000)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        
        with(NotificationManagerCompat.from(context)) {
            android.util.Log.d("NotificationService", "Mostrando notificación con ID: ${reminder.id.toInt()}")
            notify(reminder.id.toInt(), notification)
            android.util.Log.d("NotificationService", "Notificación mostrada exitosamente")
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
