package com.example.taksy.ui.screens

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taksy.R
import com.example.taksy.ui.theme.SplashLightTeal
import com.example.taksy.ui.theme.SplashLightGreen
import com.example.taksy.ui.theme.IconTeal40
import com.example.taksy.ui.theme.IconTeal80
import com.example.taksy.viewmodel.ReminderViewModel
import java.util.Calendar

/**
 * Pantalla de configuración de recordatorios diarios
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyReminderScreen(
    onBackClick: () -> Unit,
    isDarkMode: Boolean = false,
    activity: androidx.activity.ComponentActivity? = null
) {
    val context = LocalContext.current
    val reminderViewModel = remember { ReminderViewModel() }

    // Cargar preferencias guardadas
    val savedPrefs = remember { ReminderViewModel.loadPrefs(context) }
    var isDailyReminderEnabled by remember { mutableStateOf(savedPrefs.enabled) }
    var reminderTime1 by remember {
        mutableStateOf(String.format("%02d:%02d", savedPrefs.morningHour, savedPrefs.morningMinute))
    }
    var reminderTime2 by remember {
        mutableStateOf(String.format("%02d:%02d", savedPrefs.eveningHour, savedPrefs.eveningMinute))
    }
    var showSaveSuccess by remember { mutableStateOf(false) }
    var timePickerKey1 by remember { mutableStateOf(0) }
    var timePickerKey2 by remember { mutableStateOf(0) }

    // Inicializar el AlarmManager
    LaunchedEffect(Unit) {
        reminderViewModel.initializeAlarmManager(context)
    }
    
    // Mostrar TimePicker para el primer horario
    LaunchedEffect(timePickerKey1) {
        android.util.Log.d("DailyReminderScreen", "=== LaunchedEffect timePickerKey1 ===")
        android.util.Log.d("DailyReminderScreen", "timePickerKey1: $timePickerKey1")
        
        if (timePickerKey1 > 0) {
            android.util.Log.d("DailyReminderScreen", "Intentando mostrar TimePicker para hora 1")
            
            // Intentar obtener la Activity de diferentes maneras
            val activityToUse = activity ?: (context as? androidx.activity.ComponentActivity)
            android.util.Log.d("DailyReminderScreen", "Activity pasada como parámetro: $activity")
            android.util.Log.d("DailyReminderScreen", "Activity del contexto: ${context as? androidx.activity.ComponentActivity}")
            android.util.Log.d("DailyReminderScreen", "Activity a usar: $activityToUse")
            
            if (activityToUse != null && !activityToUse.isFinishing && !activityToUse.isDestroyed) {
                android.util.Log.d("DailyReminderScreen", "Activity es válida, creando TimePickerDialog")
                
                val hour = reminderTime1.split(":")[0].toInt()
                val minute = reminderTime1.split(":")[1].toInt()
                
                android.util.Log.d("DailyReminderScreen", "Hora actual: $hour:$minute")
                
                val timePickerDialog = TimePickerDialog(
                    activityToUse,
                    { _, selectedHour, selectedMinute ->
                        android.util.Log.d("DailyReminderScreen", "Hora seleccionada: $selectedHour:$selectedMinute")
                        val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                        reminderTime1 = formattedTime
                        android.util.Log.d("DailyReminderScreen", "Nueva hora 1: $formattedTime")
                    },
                    hour,
                    minute,
                    true
                )
                
                android.util.Log.d("DailyReminderScreen", "Mostrando TimePickerDialog")
                timePickerDialog.show()
            } else {
                android.util.Log.w("DailyReminderScreen", "Activity no es válida o está terminándose")
            }
        }
    }
    
    // Mostrar TimePicker para el segundo horario
    LaunchedEffect(timePickerKey2) {
        android.util.Log.d("DailyReminderScreen", "=== LaunchedEffect timePickerKey2 ===")
        android.util.Log.d("DailyReminderScreen", "timePickerKey2: $timePickerKey2")
        
        if (timePickerKey2 > 0) {
            android.util.Log.d("DailyReminderScreen", "Intentando mostrar TimePicker para hora 2")
            
            // Intentar obtener la Activity de diferentes maneras
            val activityToUse = activity ?: (context as? androidx.activity.ComponentActivity)
            android.util.Log.d("DailyReminderScreen", "Activity pasada como parámetro: $activity")
            android.util.Log.d("DailyReminderScreen", "Activity del contexto: ${context as? androidx.activity.ComponentActivity}")
            android.util.Log.d("DailyReminderScreen", "Activity a usar: $activityToUse")
            
            if (activityToUse != null && !activityToUse.isFinishing && !activityToUse.isDestroyed) {
                android.util.Log.d("DailyReminderScreen", "Activity es válida, creando TimePickerDialog")
                
                val hour = reminderTime2.split(":")[0].toInt()
                val minute = reminderTime2.split(":")[1].toInt()
                
                android.util.Log.d("DailyReminderScreen", "Hora actual: $hour:$minute")
                
                val timePickerDialog = TimePickerDialog(
                    activityToUse,
                    { _, selectedHour, selectedMinute ->
                        android.util.Log.d("DailyReminderScreen", "Hora seleccionada: $selectedHour:$selectedMinute")
                        val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                        reminderTime2 = formattedTime
                        android.util.Log.d("DailyReminderScreen", "Nueva hora 2: $formattedTime")
                    },
                    hour,
                    minute,
                    true
                )
                
                android.util.Log.d("DailyReminderScreen", "Mostrando TimePickerDialog")
                timePickerDialog.show()
            } else {
                android.util.Log.w("DailyReminderScreen", "Activity no es válida o está terminándose")
            }
        }
    }
    
    // Función para guardar recordatorios
    fun saveReminders() {
        if (isDailyReminderEnabled) {
            val time1 = reminderTime1.split(":")
            val time2 = reminderTime2.split(":")
            
            reminderViewModel.scheduleDailyReminders(
                context = context,
                morningHour = time1[0].toInt(),
                morningMinute = time1[1].toInt(),
                eveningHour = time2[0].toInt(),
                eveningMinute = time2[1].toInt(),
                enabled = isDailyReminderEnabled
            )
        } else {
            reminderViewModel.cancelDailyReminders(context)
        }
        showSaveSuccess = true
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.reminders_settings),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tarjeta principal con gradiente púrpura
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    SplashLightTeal,    // Verde-azul suave
                                    SplashLightGreen    // Verde suave
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Icono de campana dorado
                        Card(
                            modifier = Modifier.size(60.dp),
                            shape = RoundedCornerShape(30.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFD700) // Dorado
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = stringResource(R.string.notifications_title),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = IconTeal40,
                            fontSize = 24.sp
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = stringResource(R.string.notifications_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = IconTeal40.copy(alpha = 0.8f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Sección de recordatorios diarios
            Text(
                text = stringResource(R.string.daily_reminders),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = IconTeal40
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tarjeta de configuración de recordatorios
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) Color(0xFF2D2D2D) else Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Título y toggle de recordatorios diarios
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = IconTeal40,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = stringResource(R.string.daily_reminders),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = IconTeal40
                                )
                                Text(
                                    text = stringResource(R.string.daily_reminders_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Switch(
                            checked = isDailyReminderEnabled,
                            onCheckedChange = { isDailyReminderEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = IconTeal40,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color.Gray
                            )
                        )
                    }
                    
                    if (isDailyReminderEnabled) {
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Configuración de horarios
                        Text(
                            text = stringResource(R.string.reminder_times),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Primer horario
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = IconTeal40,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.reminder_time_1),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            OutlinedButton(
                                onClick = { 
                                    android.util.Log.d("DailyReminderScreen", "=== CLICK EN BOTÓN HORA 1 ===")
                                    android.util.Log.d("DailyReminderScreen", "Antes: timePickerKey1 = $timePickerKey1")
                                    timePickerKey1++
                                    android.util.Log.d("DailyReminderScreen", "Después: timePickerKey1 = $timePickerKey1")
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = IconTeal40
                                ),
                                border = BorderStroke(1.dp, IconTeal40),
                                modifier = Modifier.widthIn(min = 80.dp)
                            ) {
                                Text(
                                    text = reminderTime1,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Segundo horario
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = IconTeal40,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.reminder_time_2),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            OutlinedButton(
                                onClick = { 
                                    android.util.Log.d("DailyReminderScreen", "=== CLICK EN BOTÓN HORA 2 ===")
                                    android.util.Log.d("DailyReminderScreen", "Antes: timePickerKey2 = $timePickerKey2")
                                    timePickerKey2++
                                    android.util.Log.d("DailyReminderScreen", "Después: timePickerKey2 = $timePickerKey2")
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = IconTeal40
                                ),
                                border = BorderStroke(1.dp, IconTeal40),
                                modifier = Modifier.widthIn(min = 80.dp)
                            ) {
                                Text(
                                    text = reminderTime2,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Botón de guardar
                        Button(
                            onClick = { saveReminders() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IconTeal40
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.save_reminders),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Información adicional
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SplashLightTeal.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = IconTeal40,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.reminders_info),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
        }
    }
    
    // Mostrar mensaje de éxito
    if (showSaveSuccess) {
        LaunchedEffect(showSaveSuccess) {
            kotlinx.coroutines.delay(2000)
            showSaveSuccess = false
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(bottom = 80.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = SplashLightGreen
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = IconTeal40,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isDailyReminderEnabled) {
                            "Recordatorios guardados correctamente"
                        } else {
                            "Recordatorios desactivados"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = IconTeal40,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
