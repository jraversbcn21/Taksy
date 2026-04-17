package com.example.taksy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.taksy.R
import com.example.taksy.data.Reminder
import com.example.taksy.data.TipoRecordatorio
import com.example.taksy.ui.components.AddReminderDialog
import com.example.taksy.ui.components.SimpleReminderItem
import com.example.taksy.service.NotificationService
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pantalla dedicada para gestionar recordatorios
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    reminders: List<Reminder>,
    onBackClick: () -> Unit,
    onAddReminder: (String, String?, Date, TipoRecordatorio) -> Unit,
    onToggleReminder: (Long, Boolean) -> Unit,
    onDeleteReminder: (Reminder) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Recordatorios",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    // Botón de prueba de notificaciones inmediatas
                    IconButton(
                        onClick = {
                            // Crear un recordatorio de prueba
                            val testReminder = Reminder(
                                id = 999,
                                taskId = 0,
                                titulo = "Prueba de Notificación",
                                descripcion = "Esta es una notificación de prueba",
                                fechaRecordatorio = Date(),
                                tipoRecordatorio = TipoRecordatorio.UNA_VEZ,
                                activo = true
                            )
                            
                            val testTask = com.example.taksy.data.Task(
                                id = 0,
                                titulo = "Tarea de Prueba",
                                fechaCreacion = Date(),
                                estado = com.example.taksy.data.TaskEstado.PENDIENTE
                            )
                            
                            val notificationService = NotificationService(context)
                            notificationService.showReminderNotification(testReminder, testTask)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Probar Notificación",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // Botón de prueba de recordatorio programado (5 segundos)
                    IconButton(
                        onClick = {
                            // Crear un recordatorio que se active en 5 segundos
                            val futureDate = Date(System.currentTimeMillis() + 5000) // 5 segundos
                            onAddReminder(
                                "Prueba Programada",
                                "Este recordatorio se activará en 5 segundos",
                                futureDate,
                                TipoRecordatorio.UNA_VEZ
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Probar Recordatorio Programado",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Añadir recordatorio"
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (reminders.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No hay recordatorios",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Toca el botón + para crear uno",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            } else {
                items(
                    items = reminders,
                    key = { reminder -> "reminder_${reminder.id}" }
                ) { reminder ->
                    SimpleReminderItem(
                        reminder = reminder,
                        onToggleStatus = onToggleReminder,
                        onDelete = onDeleteReminder,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
    
    // Diálogo para añadir recordatorio
    if (showAddDialog) {
        AddReminderDialog(
            onDismiss = { showAddDialog = false },
            onAddReminder = { titulo, descripcion, fecha, tipo ->
                onAddReminder(titulo, descripcion, fecha, tipo)
                showAddDialog = false
            }
        )
    }
}
