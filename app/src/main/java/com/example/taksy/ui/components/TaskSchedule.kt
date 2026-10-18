package com.example.taksy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.taksy.R
import com.example.taksy.domain.ReminderLead
import com.example.taksy.ui.theme.HomePalette
import com.example.taksy.ui.theme.PlusJakartaSans
import com.example.taksy.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/*
 * Fecha de una tarea y su aviso, compartidos por el detalle de la tarea y la campana de la lista.
 * El aviso no es una hora suelta: se elige respecto a la fecha (ReminderLead) y se mueve con ella.
 */

/** Elección de aviso en la UI: ninguno, una opción, o la hora personalizada de un recordatorio antiguo. */
sealed interface LeadChoice {
    data object None : LeadChoice
    data class Lead(val lead: ReminderLead) : LeadChoice
    data object Custom : LeadChoice

    companion object {
        fun of(due: Date?, reminder: Date?): LeadChoice = when {
            reminder == null -> None
            due == null -> Custom
            else -> ReminderLead.match(due, reminder)?.let { Lead(it) } ?: Custom
        }
    }
}

/** "sáb, 18 oct · 09:00"; sin hora si es 00:00 y con año si no es el actual. */
@Composable
fun formatDueDateTime(date: Date): String {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    return remember(date, locale) {
        val cal = Calendar.getInstance().apply { time = date }
        val sameYear = cal.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR)
        val day = SimpleDateFormat(if (sameYear) "EEE, d MMM" else "EEE, d MMM yyyy", locale).format(date)
        val hasTime = cal.get(Calendar.HOUR_OF_DAY) != 0 || cal.get(Calendar.MINUTE) != 0
        if (hasTime) "$day · ${String.format(Locale.ROOT, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))}" else day
    }
}

/** Día (calendario) y después hora, con "Sin hora" (00:00). */
@Composable
fun DueDatePickerFlow(
    palette: HomePalette,
    accent: Color,
    initial: Date?,
    onPicked: (Date) -> Unit,
    onDismiss: () -> Unit
) {
    var pickedDayUtc by remember { mutableStateOf<Long?>(null) }
    val dayUtc = pickedDayUtc
    if (dayUtc == null) {
        HomeDatePickerDialog(
            palette = palette,
            accent = accent,
            initialUtcMillis = DateUtils.toUtcDay(initial ?: Date()),
            onConfirm = { pickedDayUtc = it },
            onDismiss = onDismiss
        )
    } else {
        val current = initial?.let { Calendar.getInstance().apply { time = it } }
        HomeTimePickerDialog(
            palette = palette,
            accent = accent,
            initialHour = current?.get(Calendar.HOUR_OF_DAY) ?: 9,
            initialMinute = current?.get(Calendar.MINUTE) ?: 0,
            onConfirm = { hour, minute -> onPicked(DateUtils.combineUtcDay(dayUtc, hour, minute)) },
            onDismiss = onDismiss,
            dismissText = stringResource(R.string.task_no_time),
            onDismissButton = { onPicked(DateUtils.combineUtcDay(dayUtc, 0, 0)) }
        )
    }
}

private val leadLabels = mapOf(
    ReminderLead.AT_TIME to R.string.reminder_at_time,
    ReminderLead.MINUTES_10 to R.string.reminder_10_min,
    ReminderLead.HOUR_1 to R.string.reminder_1_hour,
    ReminderLead.DAY_1 to R.string.reminder_1_day,
    ReminderLead.SAME_DAY_9 to R.string.reminder_same_day_9,
    ReminderLead.DAY_BEFORE_9 to R.string.reminder_day_before_9
)

/**
 * Pastillas "Avisarme": no, o una antelación respecto a [due]. Debajo, la hora exacta del aviso.
 * Sin fecha no se puede elegir aviso (salvo quitar uno antiguo).
 */
@Composable
fun ReminderLeadPicker(
    due: Date?,
    choice: LeadChoice,
    reminderTime: Date?,
    palette: HomePalette,
    accent: Color,
    onSelect: (LeadChoice) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        if (due == null && choice == LeadChoice.None) {
            OptionLabel(stringResource(R.string.reminder_notify), palette)
            Text(
                text = stringResource(R.string.reminder_needs_date),
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = palette.placeholder
            )
            return@Column
        }
        val options = buildList<PillOption<LeadChoice>> {
            add(PillOption<LeadChoice>(LeadChoice.None, R.string.reminder_none, palette.muted))
            // Justo tras "No": la fila hace scroll y al final no se vería que está seleccionado
            if (choice == LeadChoice.Custom) add(PillOption<LeadChoice>(LeadChoice.Custom, R.string.reminder_custom, accent))
            due?.let { d -> ReminderLead.optionsFor(d).forEach { add(PillOption<LeadChoice>(LeadChoice.Lead(it), leadLabels.getValue(it), accent)) } }
        }
        OptionPills(
            label = stringResource(R.string.reminder_notify),
            options = options,
            selected = choice,
            onSelect = onSelect,
            palette = palette
        )
        if (choice != LeadChoice.None && reminderTime != null) {
            val passed = reminderTime.before(Date())
            Text(
                text = stringResource(if (passed) R.string.reminder_at_passed else R.string.reminder_at, formatDueDateTime(reminderTime)),
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = if (passed) palette.placeholder else palette.muted
            )
        }
    }
}

@Composable
private fun OptionLabel(text: String, palette: HomePalette) {
    Text(
        text = text.uppercase(),
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 11.sp,
        letterSpacing = 1.sp,
        color = palette.muted
    )
}

/** Hora del aviso que corresponde a una elección (la personalizada conserva su antelación). */
fun reminderTimeFor(due: Date?, choice: LeadChoice, original: Date?, originalDue: Date?): Date? = when (choice) {
    LeadChoice.None -> null
    is LeadChoice.Lead -> due?.let { choice.lead.reminderFor(it) }
    LeadChoice.Custom -> if (due != null && originalDue != null && original != null) {
        Date(due.time + (original.time - originalDue.time))
    } else original
}

/**
 * Diálogo de la campana de la lista: fecha y aviso de una tarea a la vez. Si la tarea no tiene
 * fecha, se abre directamente el calendario.
 */
@Composable
fun TaskScheduleDialog(
    taskTitle: String,
    initialDue: Date?,
    reminder: Date?,
    palette: HomePalette,
    accent: Color,
    onSave: (due: Date?, choice: LeadChoice) -> Unit,
    onDismiss: () -> Unit
) {
    // El recordatorio llega por un Flow que empieza vacío: se reinicia el estado cuando aparece
    key(reminder) {
        var due by remember { mutableStateOf(initialDue) }
        var choice by remember { mutableStateOf(LeadChoice.of(initialDue, reminder)) }
        var pickingDate by remember { mutableStateOf(initialDue == null && reminder == null) }

        Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            HomeDialogSurface(palette) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DialogTitle(stringResource(R.string.task_schedule_title), palette)
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
                        .clickable { pickingDate = true }
                        .padding(start = 12.dp, top = 6.dp, bottom = 6.dp)
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
                        text = due?.let { formatDueDateTime(it) } ?: stringResource(R.string.task_no_due_date),
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (due != null) palette.ink else palette.placeholder,
                        modifier = Modifier.weight(1f)
                    )
                    if (due != null) {
                        IconButton(onClick = {
                            due = null
                            choice = LeadChoice.None
                        }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.delete), tint = palette.muted)
                        }
                    } else {
                        Spacer(Modifier.height(48.dp))
                    }
                }

                ReminderLeadPicker(
                    due = due,
                    choice = choice,
                    reminderTime = reminderTimeFor(due, choice, reminder, initialDue),
                    palette = palette,
                    accent = accent,
                    onSelect = { choice = it }
                )

                DialogButtons(
                    confirmText = stringResource(R.string.save),
                    confirmColor = accent,
                    palette = palette,
                    onDismiss = onDismiss,
                    onConfirm = { onSave(due, choice) }
                )
            }
        }

        if (pickingDate) {
            DueDatePickerFlow(
                palette = palette,
                accent = accent,
                initial = due,
                onPicked = { picked ->
                    val wasAllDay = due?.let { ReminderLead.isAllDay(it) }
                    due = picked
                    pickingDate = false
                    choice = when (val c = choice) {
                        // Desde la campana se viene a por un aviso: sin elección previa se propone el primero
                        LeadChoice.None -> LeadChoice.Lead(ReminderLead.optionsFor(picked).first())
                        is LeadChoice.Lead -> if (c.lead.allDay == ReminderLead.isAllDay(picked)) c
                        else LeadChoice.Lead(ReminderLead.optionsFor(picked).first())
                        LeadChoice.Custom -> if (wasAllDay == ReminderLead.isAllDay(picked)) c
                        else LeadChoice.Lead(ReminderLead.optionsFor(picked).first())
                    }
                },
                onDismiss = { pickingDate = false }
            )
        }
    }
}
