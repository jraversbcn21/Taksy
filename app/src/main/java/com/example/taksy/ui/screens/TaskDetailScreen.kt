package com.example.taksy.ui.screens

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import kotlinx.coroutines.delay
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
import com.example.taksy.ui.components.DueDatePickerFlow
import com.example.taksy.ui.components.LeadChoice
import com.example.taksy.ui.components.ReminderLeadPicker
import com.example.taksy.ui.components.formatDueDateTime
import com.example.taksy.domain.ReminderLead
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
    onRename: (String) -> Unit,
    onUpdateDescription: (String?) -> Unit,
    onUpdateDueDate: (Date?) -> Unit,
    onSetReminderLead: (ReminderLead?) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = if (isDarkMode) HomePalette.Dark else HomePalette.Light
    val accent = remember(category?.color) { category?.accentColor() ?: palette.brand }
    val context = LocalContext.current
    val isCompleted = task.estado == TaskEstado.COMPLETADA

    var addingSubtask by remember { mutableStateOf(false) }
    var newSubtaskTitle by remember { mutableStateOf("") }
    var pickingDate by remember { mutableStateOf(false) }
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
                    TitleField(task, onRename, palette, accent)
                    DescriptionField(task, onUpdateDescription, palette, accent)
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
                        value = due?.let { formatDueDateTime(it) } ?: stringResource(R.string.task_no_due_date),
                        valueColor = when {
                            due == null -> palette.placeholder
                            overdue -> if (palette.isDark) lerp(DueOverdueRed, Color.White, 0.25f) else DueOverdueRed
                            else -> palette.ink
                        },
                        accent = accent,
                        palette = palette,
                        onClick = { pickingDate = true },
                        onClear = if (due != null) ({ onUpdateDueDate(null) }) else null
                    )
                    HorizontalDivider(color = palette.border, thickness = 1.dp)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // El aviso se elige respecto a la fecha y se mueve con ella
                        ReminderLeadPicker(
                            due = due,
                            choice = LeadChoice.of(due, activeReminder?.fechaRecordatorio),
                            reminderTime = activeReminder?.fechaRecordatorio,
                            palette = palette,
                            accent = accent,
                            onSelect = { choice ->
                                when (choice) {
                                    LeadChoice.None -> onSetReminderLead(null)
                                    is LeadChoice.Lead -> {
                                        onSetReminderLead(choice.lead)
                                        // El aviso se guarda igual; sin el permiso solo puede llegar con retraso
                                        showExactAlarmDialog = AlarmPolicy.needsExactAlarmPermission(context)
                                    }
                                    LeadChoice.Custom -> Unit
                                }
                            }
                        )
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

            // Sin subtareas solo queda la fila "+ Añadir subtarea" (y nada si la tarea está completada)
            if (subtasks.isNotEmpty()) {
                item(key = "subtasks_label") {
                    SubtasksHeader(doneSubtasks, subtasks.size, accent, palette, modifier = Modifier.padding(top = 10.dp))
                }
            }

            if (subtasks.isNotEmpty() || !isCompleted) item(key = "subtasks") {
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
        DueDatePickerFlow(
            palette = palette,
            accent = accent,
            initial = task.fechaVencimiento,
            onPicked = {
                pickingDate = false
                onUpdateDueDate(it)
            },
            onDismiss = { pickingDate = false }
        )
    }

    if (showExactAlarmDialog) {
        ExactAlarmPermissionDialog(palette = palette, onDismiss = { showExactAlarmDialog = false })
    }
}

/**
 * Título editable en el sitio, con el estilo del encabezado. Se guarda como la descripción;
 * un título vacío no se guarda y al salir del campo vuelve el anterior.
 */
@Composable
private fun TitleField(task: Task, onRename: (String) -> Unit, palette: HomePalette, accent: Color) {
    var text by remember(task.id) { mutableStateOf(task.titulo) }
    val latestTask by rememberUpdatedState(task)
    val latestText by rememberUpdatedState(text)
    val latestOnSave by rememberUpdatedState(onRename)
    val focusManager = LocalFocusManager.current

    fun save() {
        val value = latestText.trim()
        if (value.isNotEmpty() && value != latestTask.titulo) latestOnSave(value)
    }

    LaunchedEffect(text) {
        delay(600)
        save()
    }
    DisposableEffect(task.id) { onDispose { save() } }

    BasicTextField(
        // Una línea lógica: un salto de línea pegado se convierte en espacio
        value = text,
        onValueChange = { text = it.replace('\n', ' ') },
        cursorBrush = SolidColor(accent),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        textStyle = TextStyle(
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 30.sp,
            lineHeight = 34.sp,
            letterSpacing = (-1).sp,
            color = palette.ink
        ),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { if (!it.isFocused && text.isBlank()) text = latestTask.titulo }
    )
}

/**
 * Descripción de la tarea, de varias líneas, bajo el título. Se guarda sola al dejar de escribir
 * (y al salir de la pantalla); el ViewModel solo cambia ese campo, así no pisa otros cambios.
 */
@Composable
private fun DescriptionField(task: Task, onSave: (String?) -> Unit, palette: HomePalette, accent: Color) {
    var text by remember(task.id) { mutableStateOf(task.descripcion.orEmpty()) }
    val latestTask by rememberUpdatedState(task)
    val latestText by rememberUpdatedState(text)
    val latestOnSave by rememberUpdatedState(onSave)

    fun save() {
        val value = latestText.trim().ifEmpty { null }
        if (value != latestTask.descripcion) latestOnSave(value)
    }

    LaunchedEffect(text) {
        delay(600)
        save()
    }
    DisposableEffect(task.id) { onDispose { save() } }

    BasicTextField(
        value = text,
        onValueChange = { text = it },
        cursorBrush = SolidColor(accent),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        textStyle = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 23.sp, color = palette.muted),
        decorationBox = { inner ->
            Box {
                if (text.isEmpty()) {
                    Text(
                        text = stringResource(R.string.task_description_hint),
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        lineHeight = 23.sp,
                        color = palette.placeholder
                    )
                }
                inner()
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
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
