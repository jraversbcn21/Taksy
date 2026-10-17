package com.example.taksy.ui.screens

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taksy.R
import com.example.taksy.data.preferences.PreferencesRepository
import com.example.taksy.service.AlarmPolicy
import com.example.taksy.service.DailyReminderManager
import com.example.taksy.ui.components.ExactAlarmPermissionDialog
import com.example.taksy.ui.theme.HomePalette
import com.example.taksy.ui.theme.PlusJakartaSans
import java.util.Locale

/**
 * Recordatorios diarios: un resumen de tareas pendientes por la mañana y otro por la tarde.
 * Cada cambio se guarda y reprograma al momento (sin botón de guardar), igual que en Configuración.
 * Mismo lenguaje visual que la pantalla de inicio (HomePalette + Plus Jakarta Sans).
 */
@Composable
fun DailyReminderScreen(
    onBackClick: () -> Unit,
    isDarkMode: Boolean = false
) {
    val palette = if (isDarkMode) HomePalette.Dark else HomePalette.Light
    val context = LocalContext.current
    var prefs by remember { mutableStateOf(DailyReminderManager.loadPrefs(context)) }
    var editingMorning by remember { mutableStateOf<Boolean?>(null) }
    var showExactAlarmDialog by remember { mutableStateOf(false) }

    // Guarda las preferencias y reprograma (o cancela) las dos alarmas
    fun apply(newPrefs: PreferencesRepository.DailyReminderPrefs) {
        prefs = newPrefs
        DailyReminderManager.scheduleDailyReminders(
            context = context,
            morningHour = newPrefs.morningHour,
            morningMinute = newPrefs.morningMinute,
            eveningHour = newPrefs.eveningHour,
            eveningMinute = newPrefs.eveningMinute,
            enabled = newPrefs.enabled
        )
    }

    val summary = if (prefs.enabled) {
        stringResource(
            R.string.settings_reminders_on,
            formatTime(prefs.morningHour, prefs.morningMinute),
            formatTime(prefs.eveningHour, prefs.eveningMinute)
        )
    } else {
        stringResource(R.string.settings_reminders_off)
    }

    Scaffold(containerColor = palette.background) { paddingValues ->
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 32.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                IconButton(onClick = onBackClick, modifier = Modifier.offset(x = (-12).dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = palette.ink
                    )
                }
                Text(
                    text = stringResource(R.string.reminders_settings),
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 34.sp,
                    lineHeight = 37.sp,
                    letterSpacing = (-1).sp,
                    color = palette.ink
                )
                Text(
                    text = stringResource(R.string.daily_reminders_description),
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = palette.muted
                )
            }

            // ── Activar ──
            ReminderCard(palette) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Switch) {
                            val enabling = !prefs.enabled
                            apply(prefs.copy(enabled = enabling))
                            if (enabling) showExactAlarmDialog = AlarmPolicy.needsExactAlarmPermission(context)
                        }
                        .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF00897B))
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.daily_reminders),
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = palette.ink
                        )
                        Text(
                            text = summary,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = palette.muted
                        )
                    }
                    // El cambio lo gestiona la fila entera (clickable) para que toda ella sea táctil
                    Switch(
                        checked = prefs.enabled,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = palette.brand,
                            checkedBorderColor = palette.brand,
                            uncheckedThumbColor = palette.muted,
                            uncheckedTrackColor = palette.track,
                            uncheckedBorderColor = palette.border
                        )
                    )
                }
            }

            // ── Horarios ──
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(stringResource(R.string.reminder_times), palette)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .height(IntrinsicSize.Min)
                        .alpha(if (prefs.enabled) 1f else 0.45f)
                ) {
                    TimeTile(
                        label = stringResource(R.string.reminder_time_1),
                        time = formatTime(prefs.morningHour, prefs.morningMinute),
                        accent = Color(0xFFF9A825),
                        palette = palette,
                        enabled = prefs.enabled,
                        onClick = { editingMorning = true },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    TimeTile(
                        label = stringResource(R.string.reminder_time_2),
                        time = formatTime(prefs.eveningHour, prefs.eveningMinute),
                        accent = Color(0xFF5C6BC0),
                        palette = palette,
                        enabled = prefs.enabled,
                        onClick = { editingMorning = false },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }

            // ── Información ──
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = palette.muted, modifier = Modifier.size(18.dp))
                Text(
                    text = stringResource(R.string.reminders_info),
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = palette.muted
                )
            }
        }
    }

    editingMorning?.let { morning ->
        ReminderTimePickerDialog(
            initialHour = if (morning) prefs.morningHour else prefs.eveningHour,
            initialMinute = if (morning) prefs.morningMinute else prefs.eveningMinute,
            onConfirm = { hour, minute ->
                apply(
                    if (morning) prefs.copy(morningHour = hour, morningMinute = minute)
                    else prefs.copy(eveningHour = hour, eveningMinute = minute)
                )
                editingMorning = null
            },
            onDismiss = { editingMorning = null }
        )
    }

    if (showExactAlarmDialog) {
        ExactAlarmPermissionDialog(onDismiss = { showExactAlarmDialog = false })
    }
}

private fun formatTime(hour: Int, minute: Int) = String.format(Locale.ROOT, "%02d:%02d", hour, minute)

@Composable
private fun SectionLabel(text: String, palette: HomePalette) {
    Text(
        text = text.uppercase(),
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 13.sp,
        letterSpacing = 1.sp,
        color = palette.muted
    )
}

@Composable
private fun ReminderCard(palette: HomePalette, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.surface)
            .border(1.dp, palette.border, shape),
        content = content
    )
}

@Composable
private fun TimeTile(
    label: String,
    time: String,
    accent: Color,
    palette: HomePalette,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .clip(shape)
            .background(palette.surface)
            .border(1.dp, palette.border, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp)
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Text(
            text = label,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = palette.muted,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = time,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 32.sp,
            letterSpacing = (-1).sp,
            color = palette.ink
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = DateFormat.is24HourFormat(context)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
