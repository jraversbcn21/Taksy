package com.example.taksy.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.taksy.R
import com.example.taksy.data.Reminder
import com.example.taksy.ui.theme.DueOverdueRed
import com.example.taksy.ui.theme.HomePalette
import com.example.taksy.ui.theme.PlusJakartaSans
import com.example.taksy.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/**
 * Recordatorio rápido de una tarea (uno por tarea, de un solo disparo): día + hora.
 * El día va en medianoche UTC como espera el DatePicker (ver DateUtils.toUtcDay).
 */
@Composable
fun QuickReminderDialog(
    taskTitle: String,
    existingReminder: Reminder?,
    palette: HomePalette,
    accent: Color,
    onSetReminder: (Date) -> Unit,
    onDeleteReminder: () -> Unit,
    onDismiss: () -> Unit
) {
    // El recordatorio puede llegar después de la primera composición (Flow que empieza vacío):
    // el estado se reinicia cuando cambia para no quedarse con la hora por defecto.
    key(existingReminder?.id) {
        QuickReminderContent(taskTitle, existingReminder, palette, accent, onSetReminder, onDeleteReminder, onDismiss)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickReminderContent(
    taskTitle: String,
    existingReminder: Reminder?,
    palette: HomePalette,
    accent: Color,
    onSetReminder: (Date) -> Unit,
    onDeleteReminder: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val initial = remember {
        existingReminder?.fechaRecordatorio
            ?: Calendar.getInstance().apply {
                // Por defecto: dentro de una hora, en punto
                add(Calendar.HOUR_OF_DAY, 1)
                set(Calendar.MINUTE, 0)
            }.time
    }
    val initialCal = remember { Calendar.getInstance().apply { time = initial } }
    var dayUtcMillis by remember { mutableLongStateOf(DateUtils.toUtcDay(initial)) }
    var pickingDay by remember { mutableStateOf(false) }
    val timeState = rememberTimePickerState(
        initialHour = initialCal.get(Calendar.HOUR_OF_DAY),
        initialMinute = initialCal.get(Calendar.MINUTE),
        is24Hour = DateFormat.is24HourFormat(context)
    )
    val danger = if (palette.isDark) lerp(DueOverdueRed, Color.White, 0.25f) else DueOverdueRed
    val locale = LocalConfiguration.current.locales[0]
    val dayText = remember(dayUtcMillis, locale) {
        // El día está en UTC: se formatea en UTC para no desplazarlo
        SimpleDateFormat("EEEE, d MMMM yyyy", locale)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date(dayUtcMillis))
            .replaceFirstChar { it.titlecase(locale) }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        HomeDialogSurface(palette, Modifier.verticalScroll(rememberScrollState())) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DialogTitle(
                    stringResource(if (existingReminder != null) R.string.edit_reminder else R.string.set_reminder),
                    palette
                )
                Text(
                    text = taskTitle,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = palette.muted
                )
            }

            val rowShape = RoundedCornerShape(16.dp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(rowShape)
                    .background(palette.background)
                    .border(1.dp, palette.border, rowShape)
                    .clickable { pickingDay = true }
                    .padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
                }
                Text(
                    text = dayText,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = palette.ink,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = palette.muted)
            }

            TimePicker(
                state = timeState,
                colors = homeTimePickerColors(palette, accent),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            DialogButtons(
                confirmText = stringResource(R.string.save),
                confirmColor = accent,
                palette = palette,
                onDismiss = onDismiss,
                onConfirm = { onSetReminder(DateUtils.combineUtcDay(dayUtcMillis, timeState.hour, timeState.minute)) },
                leading = if (existingReminder != null) {
                    { DialogTextButton(stringResource(R.string.delete), danger, onDeleteReminder) }
                } else null
            )
        }
    }

    if (pickingDay) {
        HomeDatePickerDialog(
            palette = palette,
            accent = accent,
            initialUtcMillis = dayUtcMillis,
            onConfirm = {
                dayUtcMillis = it
                pickingDay = false
            },
            onDismiss = { pickingDay = false }
        )
    }
}
