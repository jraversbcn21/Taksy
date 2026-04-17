package com.example.taksy.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.taksy.MainActivity
import com.example.taksy.R
import com.example.taksy.data.AppDatabase
import com.example.taksy.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

/**
 * Servicio para manejar recordatorios diarios
 */
class DailyReminderService : BroadcastReceiver() {
    
    companion object {
        private const val CHANNEL_ID = "daily_reminders"
        private const val NOTIFICATION_ID_1 = 1001
        private const val NOTIFICATION_ID_2 = 1002
        
        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Recordatorios Diarios",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notificaciones diarias para recordar revisar las tareas"
                    enableLights(true)
                    enableVibration(true)
                }
                
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val reminderType = intent.getStringExtra("reminder_type") ?: "morning"
        android.util.Log.d("DailyReminderService", "=== RECIBIENDO ALARMA ===")
        android.util.Log.d("DailyReminderService", "Tipo de recordatorio: $reminderType")
        android.util.Log.d("DailyReminderService", "Tiempo actual: ${java.util.Date()}")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Obtener información de tareas pendientes
                val database = AppDatabase.getDatabase(context)
                val taskRepository = TaskRepository(
                    database.taskDao(),
                    database.subtaskDao(),
                    database.reminderDao()
                )
                val pendingTasksFlow = taskRepository.getTasksByFilter(
                    com.example.taksy.repository.TaskFilter.PENDIENTES
                )
                val pendingTasks = pendingTasksFlow.first()
                
                // Crear notificación
                android.util.Log.d("DailyReminderService", "Creando notificación con ${pendingTasks.size} tareas pendientes")
                createNotification(context, pendingTasks, reminderType)
                
            } catch (e: Exception) {
                // En caso de error, crear notificación genérica
                android.util.Log.e("DailyReminderService", "Error creando notificación: ${e.message}")
                createGenericNotification(context, reminderType)
            }
        }
    }
    
    private suspend fun createNotification(context: Context, pendingTasks: List<com.example.taksy.data.Task>, reminderType: String) {
        val title = if (reminderType == "morning") {
            context.getString(R.string.morning_reminder_title)
        } else {
            context.getString(R.string.evening_reminder_title)
        }
        
        val content = if (pendingTasks.isNotEmpty()) {
            if (pendingTasks.size == 1) {
                context.getString(R.string.reminder_with_tasks_single, pendingTasks.size)
            } else {
                context.getString(R.string.reminder_with_tasks_multiple, pendingTasks.size)
            }
        } else {
            context.getString(R.string.reminder_no_tasks)
        }
        
        showNotification(context, title, content, reminderType)
    }
    
    private fun createGenericNotification(context: Context, reminderType: String) {
        val title = if (reminderType == "morning") {
            context.getString(R.string.morning_reminder_title)
        } else {
            context.getString(R.string.evening_reminder_title)
        }
        
        val content = context.getString(R.string.reminder_generic_message)
        
        showNotification(context, title, content, reminderType)
    }
    
    private fun showNotification(context: Context, title: String, content: String, reminderType: String) {
        // Crear intent para abrir la aplicación
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Crear notificación
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ticksy_icon)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(content)
            )
            .build()
        
        // Mostrar notificación
        val notificationManager = NotificationManagerCompat.from(context)
        val notificationId = if (reminderType == "morning") NOTIFICATION_ID_1 else NOTIFICATION_ID_2
        
        try {
            android.util.Log.d("DailyReminderService", "Mostrando notificación con ID: $notificationId")
            android.util.Log.d("DailyReminderService", "Título: $title")
            android.util.Log.d("DailyReminderService", "Contenido: $content")
            notificationManager.notify(notificationId, notification)
            android.util.Log.d("DailyReminderService", "Notificación mostrada exitosamente")
        } catch (e: SecurityException) {
            // Manejar error de permisos
            android.util.Log.e("DailyReminderService", "No se pudo mostrar la notificación: ${e.message}")
        } catch (e: Exception) {
            android.util.Log.e("DailyReminderService", "Error general mostrando notificación: ${e.message}")
        }
    }
}
