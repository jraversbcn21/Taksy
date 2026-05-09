package com.example.taksy.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.taksy.R
import com.example.taksy.data.TipoRecordatorio
import com.example.taksy.ui.components.CustomDatePicker
import com.example.taksy.ui.components.CustomTimePicker
import java.util.Date
import java.util.Calendar
import java.util.Locale

/**
 * Diálogo para añadir un nuevo recordatorio
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderDialog(
    onDismiss: () -> Unit,
    onAddReminder: (String, String?, Date, TipoRecordatorio) -> Unit,
    modifier: Modifier = Modifier
) {
    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var fechaRecordatorio by remember { mutableStateOf(Date()) }
    var tipoRecordatorio by remember { mutableStateOf(TipoRecordatorio.UNA_VEZ) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = fechaRecordatorio.time
    )
    val timePickerState = rememberTimePickerState(
        initialHour = java.util.Calendar.getInstance().apply { time = fechaRecordatorio }.get(java.util.Calendar.HOUR_OF_DAY),
        initialMinute = java.util.Calendar.getInstance().apply { time = fechaRecordatorio }.get(java.util.Calendar.MINUTE)
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.add_reminder),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Título del recordatorio
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { 
                        Text(
                            stringResource(R.string.reminder_title),
                            color = MaterialTheme.colorScheme.onSurface
                        ) 
                    },
                    placeholder = {
                        Text(
                            stringResource(R.string.reminder_title_placeholder),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                
                // Descripción
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { 
                        Text(
                            stringResource(R.string.reminder_description),
                            color = MaterialTheme.colorScheme.onSurface
                        ) 
                    },
                    placeholder = {
                        Text(
                            stringResource(R.string.reminder_description_placeholder),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                
                // Fecha y hora
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.date_label))
                    }

                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.time_label))
                    }
                }
                
                // Tipo de recordatorio
                Text(
                    text = stringResource(R.string.reminder_type),
                    style = MaterialTheme.typography.labelLarge
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TipoRecordatorio.values().forEach { tipo ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = tipoRecordatorio == tipo,
                                onClick = { tipoRecordatorio = tipo }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (tipo) {
                                    TipoRecordatorio.UNA_VEZ -> stringResource(R.string.reminder_once)
                                    TipoRecordatorio.DIARIO -> stringResource(R.string.reminder_daily)
                                    TipoRecordatorio.SEMANAL -> stringResource(R.string.reminder_weekly)
                                    TipoRecordatorio.MENSUAL -> stringResource(R.string.reminder_monthly)
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (titulo.isNotBlank()) {
                        onAddReminder(titulo, descripcion.ifBlank { null }, fechaRecordatorio, tipoRecordatorio)
                        onDismiss()
                    }
                },
                enabled = titulo.isNotBlank()
            ) {
                Text(stringResource(R.string.add_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )

    // Date Picker usando DatePicker de Compose con configuración completa
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = fechaRecordatorio.time
        )
        
        AlertDialog(
            onDismissRequest = { showDatePicker = false },
            title = { 
                Text(
                    text = stringResource(R.string.select_date),
                    style = MaterialTheme.typography.headlineSmall
                ) 
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Mostrar la fecha actual seleccionada
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("es", "ES"))
                                    .format(fechaRecordatorio),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    // Calendario personalizado que muestra lunes a domingo
                    CustomDatePicker(
                        selectedDate = fechaRecordatorio,
                        onDateSelected = { newDate ->
                            // Mantener la hora actual
                            val calendar = java.util.Calendar.getInstance()
                            calendar.time = newDate
                            val currentCalendar = java.util.Calendar.getInstance()
                            currentCalendar.time = fechaRecordatorio
                            calendar.set(java.util.Calendar.HOUR_OF_DAY, currentCalendar.get(java.util.Calendar.HOUR_OF_DAY))
                            calendar.set(java.util.Calendar.MINUTE, currentCalendar.get(java.util.Calendar.MINUTE))
                            fechaRecordatorio = calendar.time
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // La fecha ya se actualiza automáticamente al seleccionar
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(horizontal = 4.dp)
        )
    }
    
    // Time Picker usando TimePicker de Compose
    if (showTimePicker) {
        val calendar = java.util.Calendar.getInstance().apply { time = fechaRecordatorio }
        val timePickerState = rememberTimePickerState(
            initialHour = calendar.get(java.util.Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(java.util.Calendar.MINUTE),
            is24Hour = true
        )
        
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { 
                Text(
                    text = stringResource(R.string.select_time),
                    style = MaterialTheme.typography.headlineSmall
                ) 
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)  // Altura fija para el contenido
                ) {
                    // Mostrar la hora actual seleccionada
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = java.text.SimpleDateFormat("HH:mm", java.util.Locale("es", "ES"))
                                    .format(fechaRecordatorio),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    // TimePicker original con agujas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TimePicker(
                            state = timePickerState,
                            modifier = Modifier
                                .size(300.dp)
                                .padding(8.dp),
                            colors = TimePickerDefaults.colors(
                                clockDialSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                                clockDialUnselectedContentColor = MaterialTheme.colorScheme.onSurface,
                                selectorColor = MaterialTheme.colorScheme.primary,
                                periodSelectorBorderColor = MaterialTheme.colorScheme.primary,
                                periodSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary,
                                periodSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surface,
                                timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary,
                                timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val calendar = java.util.Calendar.getInstance()
                        calendar.time = fechaRecordatorio
                        calendar.set(java.util.Calendar.HOUR_OF_DAY, timePickerState.hour)
                        calendar.set(java.util.Calendar.MINUTE, timePickerState.minute)
                        fechaRecordatorio = calendar.time
                        showTimePicker = false
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            modifier = Modifier
                .fillMaxWidth(0.98f)
                .padding(horizontal = 8.dp)
        )
    }
}
