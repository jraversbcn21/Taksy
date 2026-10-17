package com.example.taksy.ui.screens

import android.text.format.DateFormat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taksy.R
import com.example.taksy.data.Category
import com.example.taksy.data.Reminder
import com.example.taksy.data.Subtask
import com.example.taksy.data.Task
import com.example.taksy.data.TaskEstado
import com.example.taksy.service.AlarmPolicy
import com.example.taksy.ui.components.ExactAlarmPermissionDialog
import com.example.taksy.ui.components.OptionPills
import com.example.taksy.ui.components.QuickReminderDialog
import com.example.taksy.ui.components.taskdetail.AddSubtaskRow
import com.example.taksy.ui.components.taskdetail.SubtaskItem
import com.example.taksy.ui.components.tasksbycategory.priorityOptions
import com.example.taksy.ui.components.tasksbycategory.recurrenceOptions
import com.example.taksy.ui.theme.DueOverdueRed
import com.example.taksy.ui.theme.HomePalette
import com.example.taksy.ui.theme.PlusJakartaSans
import com.example.taksy.ui.theme.accentColor
import com.example.taksy.utils.CategoryUtils
import com.example.taksy.utils.DateUtils
import com.example.taksy.utils.getCategoryIcon
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Detalle de una tarea con el lenguaje visual de la pantalla de inicio: fecha y recordatorio,
 * prioridad, repetición y subtareas. Cada cambio se guarda al momento.
 */
@Composable
fun TaskDetailScreen(
    task: Task,
    category: Category?,
    subtasks: List<Subtask>,
    activeReminder: Reminder?,
    isDarkMode: Boolean,
    onBackClick: () -> Unit,
    onAddSubtask: (String) -> Unit,
    onToggleSubtask: (Subtask) -> Unit,
    onUpdateTask: (Task) -> Unit,
    onSetReminder: (Date) -> Unit,
    onDeleteReminder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = if (isDarkMode) HomePalette.Dark else HomePalette.Light
    val accent = remember(category?.color) { category?.accentColor() ?: palette.brand }
    val context = LocalContext.current
    val isCompleted = task.estado == TaskEstado.COMPLETADA

    var addingSubtask by remember { mutableStateOf(false) }
    var newSubtaskTitle by remember { mutableStateOf("") }
    var pickingDate by remember { mutableStateOf(false) }
    var pickedDayUtcMillis by remember { mutableStateOf<Long?>(null) }
    var editingReminder by remember { mutableStateOf(false) }
    var showExactAlarmDialog by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val allSubtasksCompletedMessage = stringResource(R.string.all_subtasks_completed)

    LaunchedEffect(addingSubtask) {
        if (addingSubtask) focusRequester.requestFocus()
    }

    // Pendientes primero; dentro de cada grupo, el orden de creación
    val sortedSubtasks = remember(subtasks) { subtasks.sortedBy { it.estado == TaskEstado.COMPLETADA } }
    val doneSubtasks = subtasks.count { it.estado == TaskEstado.COMPLETADA }

    Scaffold(
        containerColor = palette.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 48.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item(key = "back") {
                IconButton(onClick = onBackClick, modifier = Modifier.offset(x = (-12).dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = palette.ink)
                }
            }

            item(key = "title") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (category != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(accent)
                            ) {
                                Icon(getCategoryIcon(category.icono), contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                            Text(
                                text = CategoryUtils.getCategoryName(category),
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = palette.muted
                            )
                        }
                    }
                    Text(
                        text = task.titulo,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 30.sp,
                        lineHeight = 34.sp,
                        letterSpacing = (-1).sp,
                        color = palette.ink
                    )
                    if (isCompleted) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                            Text(
                                text = (task.fechaCompletada ?: task.fechaVencimiento)
                                    ?.let { stringResource(R.string.completed_on, DateUtils.formatDate(it)) }
                                    ?: stringResource(R.string.tasks_section_done),
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = accent
                            )
                        }
                    }
                }
            }

            item(key = "details") {
                DetailCard(palette, modifier = Modifier.padding(top = 8.dp)) {
                    val due = task.fechaVencimiento
                    // Vencida en cuanto pasa la hora, igual que en las tarjetas de hoy de la pantalla de inicio
                    val overdue = !isCompleted && due != null && due.before(Date())
                    DetailRow(
                        icon = Icons.Default.DateRange,
                        label = stringResource(R.string.task_due_date),
                        value = due?.let { formatDateTime(it) } ?: stringResource(R.string.task_no_due_date),
                        valueColor = when {
                            due == null -> palette.placeholder
                            overdue -> if (palette.isDark) lerp(DueOverdueRed, Color.White, 0.25f) else DueOverdueRed
                            else -> palette.ink
                        },
                        accent = accent,
                        palette = palette,
                        onClick = { pickingDate = true },
                        onClear = if (due != null) ({ onUpdateTask(task.copy(fechaVencimiento = null)) }) else null
                    )
                    HorizontalDivider(color = palette.border, thickness = 1.dp)
                    DetailRow(
                        icon = Icons.Default.Notifications,
                        label = stringResource(R.string.task_reminder),
                        value = activeReminder?.let { formatDateTime(it.fechaRecordatorio) } ?: stringResource(R.string.task_no_reminder),
                        valueColor = if (activeReminder != null) palette.ink else palette.placeholder,
                        accent = accent,
                        palette = palette,
                        onClick = { editingReminder = true },
                        onClear = if (activeReminder != null) onDeleteReminder else null
                    )
                    HorizontalDivider(color = palette.border, thickness = 1.dp)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        OptionPills(
                            label = stringResource(R.string.task_priority),
                            options = priorityOptions(palette),
                            selected = task.prioridad,
                            onSelect = { if (it != task.prioridad) onUpdateTask(task.copy(prioridad = it)) },
                            palette = palette
                        )
                        OptionPills(
                            label = stringResource(R.string.recurrence),
                            options = recurrenceOptions(palette, accent),
                            selected = task.recurrencia,
                            onSelect = { if (it != task.recurrencia) onUpdateTask(task.copy(recurrencia = it)) },
                            palette = palette
                        )
                    }
                }
            }

            item(key = "subtasks_label") {
                SubtasksHeader(doneSubtasks, subtasks.size, accent, palette, modifier = Modifier.padding(top = 10.dp))
            }

            item(key = "subtasks") {
                DetailCard(palette) {
                    sortedSubtasks.forEach { subtask ->
                        SubtaskItem(
                            subtask = subtask,
                            palette = palette,
                            accent = accent,
                            enabled = !isCompleted,
                            onToggle = {
                                val pendingBefore = subtasks.count { it.estado == TaskEstado.PENDIENTE }
                                onToggleSubtask(subtask)
                                if (pendingBefore == 1) {
                                    scope.launch { snackbarHostState.showSnackbar(allSubtasksCompletedMessage) }
                                }
                            }
                        )
                        HorizontalDivider(color = palette.border, thickness = 1.dp)
                    }
                    AddSubtaskRow(
                        // Una tarea completada no admite subtareas nuevas: se cierra el campo si estaba abierto
                        isEditing = addingSubtask && !isCompleted,
                        subtaskTitle = newSubtaskTitle,
                        onSubtaskTitleChange = { newSubtaskTitle = it },
                        onStartEditing = { addingSubtask = true },
                        onAddSubtask = { title ->
                            onAddSubtask(title)
                            newSubtaskTitle = ""
                        },
                        onCancel = {
                            newSubtaskTitle = ""
                            addingSubtask = false
                        },
                        focusRequester = focusRequester,
                        palette = palette,
                        accent = accent,
                        enabled = !isCompleted
                    )
                }
            }
        }
    }

    if (pickingDate) {
        DueDatePickerDialog(
            initialDate = task.fechaVencimiento,
            onDateSelected = { utcMillis ->
                pickingDate = false
                pickedDayUtcMillis = utcMillis
            },
            onDismiss = { pickingDate = false }
        )
    }

    pickedDayUtcMillis?.let { dayUtc ->
        val current = task.fechaVencimiento?.let { Calendar.getInstance().apply { time = it } }
        DueTimePickerDialog(
            initialHour = current?.get(Calendar.HOUR_OF_DAY) ?: 9,
            initialMinute = current?.get(Calendar.MINUTE) ?: 0,
            onConfirm = { hour, minute ->
                onUpdateTask(task.copy(fechaVencimiento = combine(dayUtc, hour, minute)))
                pickedDayUtcMillis = null
            },
            // "Sin hora" se guarda como 00:00, igual que en el resto de la app
            onNoTime = {
                onUpdateTask(task.copy(fechaVencimiento = combine(dayUtc, 0, 0)))
                pickedDayUtcMillis = null
            },
            onDismiss = { pickedDayUtcMillis = null }
        )
    }

    if (editingReminder) {
        QuickReminderDialog(
            taskTitle = task.titulo,
            existingReminder = activeReminder,
            onSetReminder = { date ->
                onSetReminder(date)
                editingReminder = false
                // El recordatorio se guarda igual; sin el permiso solo puede llegar con retraso
                showExactAlarmDialog = AlarmPolicy.needsExactAlarmPermission(context)
            },
            onDeleteReminder = {
                onDeleteReminder()
                editingReminder = false
            },
            onDismiss = { editingReminder = false }
        )
    }

    if (showExactAlarmDialog) {
        ExactAlarmPermissionDialog(onDismiss = { showExactAlarmDialog = false })
    }
}

/** Fecha del DatePicker (medianoche UTC) + hora local → Date local. */
private fun combine(dayUtcMillis: Long, hour: Int, minute: Int): Date {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = dayUtcMillis }
    return Calendar.getInstance().apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), hour, minute)
    }.time
}

/** Día local de [date] como medianoche UTC, que es lo que espera el DatePicker. */
private fun toUtcDay(date: Date): Long {
    val local = Calendar.getInstance().apply { time = date }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

/** "sáb, 18 oct · 09:00"; sin hora si es 00:00 y con año si no es el actual. */
@Composable
private fun formatDateTime(date: Date): String {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    return remember(date, locale) {
        val cal = Calendar.getInstance().apply { time = date }
        val sameYear = cal.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR)
        val day = SimpleDateFormat(if (sameYear) "EEE, d MMM" else "EEE, d MMM yyyy", locale).format(date)
        val hasTime = cal.get(Calendar.HOUR_OF_DAY) != 0 || cal.get(Calendar.MINUTE) != 0
        if (hasTime) "$day · ${String.format(Locale.ROOT, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))}" else day
    }
}

@Composable
private fun DetailCard(palette: HomePalette, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.surface)
            .border(1.dp, palette.border, shape),
        content = content
    )
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color,
    accent: Color,
    palette: HomePalette,
    onClick: () -> Unit,
    onClear: (() -> Unit)?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp)
            .heightIn(min = 44.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(accent.copy(alpha = if (palette.isDark) 0.22f else 0.12f))
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
            Text(label, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = palette.muted)
            Text(value, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = valueColor)
        }
        if (onClear != null) {
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.delete), tint = palette.muted, modifier = Modifier.size(20.dp))
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }
    }
}

@Composable
private fun SubtasksHeader(done: Int, total: Int, accent: Color, palette: HomePalette, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(if (total > 0) done.toFloat() / total else 0f, tween(450), label = "subtasks")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.subtasks_title).uppercase(),
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                letterSpacing = 1.sp,
                color = palette.muted,
                modifier = Modifier.weight(1f)
            )
            if (total > 0) {
                Text(
                    text = stringResource(R.string.home_done_of, done, total),
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = palette.muted
                )
            }
        }
        if (total > 0) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(palette.track)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(accent)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DueDatePickerDialog(initialDate: Date?, onDateSelected: (Long) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = toUtcDay(initialDate ?: Date()))
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let(onDateSelected) },
                enabled = state.selectedDateMillis != null
            ) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DueTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onNoTime: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val state = rememberTimePickerState(initialHour, initialMinute, DateFormat.is24HourFormat(context))
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(state = state) },
        confirmButton = { TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = onNoTime) { Text(stringResource(R.string.task_no_time)) } }
    )
}
