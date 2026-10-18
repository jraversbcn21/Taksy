package com.example.taksy.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import com.example.taksy.R
import com.example.taksy.data.Task
import com.example.taksy.data.TaskEstado
import com.example.taksy.data.TaskPrioridad
import com.example.taksy.data.TaskRecurrencia
import com.example.taksy.ui.theme.DueOverdueRed
import com.example.taksy.ui.theme.DueTodayOrange
import com.example.taksy.ui.theme.DueTomorrowOrange
import com.example.taksy.ui.theme.HomePalette
import com.example.taksy.ui.theme.PlusJakartaSans
import com.example.taksy.ui.theme.PriorityHighRed
import com.example.taksy.ui.theme.PriorityLowGreen
import com.example.taksy.ui.theme.PriorityMediumOrange
import com.example.taksy.ui.theme.SwipeDeleteBg
import com.example.taksy.utils.DateUtils
import com.example.taksy.utils.DueDateStatus

/**
 * Fila de tarea en la lista de una categoría, con el lenguaje visual de la pantalla de inicio.
 * Deslizar a la izquierda pide confirmación para borrar; mantener pulsado edita el título.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TaskListItem(
    task: Task,
    palette: HomePalette,
    accent: Color,
    pendingSubtasksCount: Int = 0,
    hasActiveReminder: Boolean = false,
    onTaskClick: (Task) -> Unit,
    onTaskToggle: (Task) -> Unit,
    onTaskDelete: (Task) -> Unit,
    onShowToast: (String) -> Unit = {},
    onReminderClick: (Task) -> Unit = {},
    onEditTask: (Task) -> Unit = {},
    highlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Resalte breve con el color de la categoría (p. ej. la tarea recién añadida)
    val highlight by animateFloatAsState(if (highlighted) 1f else 0f, tween(if (highlighted) 250 else 900), label = "highlight")
    var showEditDialog by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isToggling by remember { mutableStateOf(false) }

    LaunchedEffect(task.estado) { isToggling = false }

    val isCompleted = task.estado == TaskEstado.COMPLETADA
    val isLocked = isCompleted && pendingSubtasksCount == 0
    val pendingSubtasksMessage = stringResource(R.string.pending_subtasks_message)
    val shape = RoundedCornerShape(18.dp)

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) showDeleteDialog = true
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(SwipeDeleteBg)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete_task),
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(if (isCompleted) palette.doneCard else lerp(palette.surface, accent, highlight * if (palette.isDark) 0.22f else 0.12f))
                .border(if (highlight > 0f) 1.5.dp else 1.dp, lerp(palette.border, accent, highlight), shape)
                .combinedClickable(
                    onClick = { onTaskClick(task) },
                    onLongClick = {
                        editTitle = task.titulo
                        showEditDialog = true
                    }
                )
                .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
        ) {
            // Círculo para completar (mismo estilo que las tarjetas de hoy)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(enabled = !isLocked && !isToggling, role = Role.Checkbox) {
                        if (pendingSubtasksCount > 0) {
                            onShowToast(pendingSubtasksMessage)
                        } else {
                            isToggling = true
                            onTaskToggle(task)
                        }
                    }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isCompleted) accent else Color.Transparent)
                        .then(if (isCompleted) Modifier else Modifier.border(2.dp, accent, CircleShape))
                ) {
                    if (isCompleted) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp, top = 8.dp, bottom = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (task.prioridad != TaskPrioridad.NINGUNA && !isCompleted) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .background(priorityColor(task.prioridad), CircleShape)
                        )
                        Spacer(Modifier.width(7.dp))
                    }
                    Text(
                        text = task.titulo,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        lineHeight = 19.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isCompleted) palette.doneText else palette.ink,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                }
                TaskMeta(task, isCompleted, palette, accent)
            }

            IconButton(onClick = { onReminderClick(task) }) {
                Icon(
                    imageVector = if (hasActiveReminder) Icons.Default.Notifications else Icons.Outlined.Notifications,
                    contentDescription = stringResource(R.string.set_reminder),
                    tint = if (hasActiveReminder) accent else palette.placeholder.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }

            if (pendingSubtasksCount > 0) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = if (palette.isDark) 0.22f else 0.14f))
                ) {
                    Text(
                        text = pendingSubtasksCount.toString(),
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        color = if (palette.isDark) lerp(accent, Color.White, 0.4f) else accent
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        HomeMessageDialog(
            title = stringResource(R.string.delete_task_title),
            message = stringResource(R.string.delete_task_message),
            confirmText = stringResource(R.string.delete),
            confirmColor = if (palette.isDark) lerp(DueOverdueRed, Color.White, 0.25f) else DueOverdueRed,
            palette = palette,
            onConfirm = {
                onTaskDelete(task)
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (showEditDialog) {
        RenameTaskDialog(
            initialTitle = editTitle,
            palette = palette,
            accent = accent,
            onConfirm = { title ->
                onEditTask(task.copy(titulo = title))
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false }
        )
    }
}

@Composable
private fun RenameTaskDialog(
    initialTitle: String,
    palette: HomePalette,
    accent: Color,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(TextFieldValue(initialTitle, TextRange(initialTitle.length))) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val trimmed = title.text.trim()
    val fieldShape = RoundedCornerShape(16.dp)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        HomeDialogSurface(palette) {
            DialogTitle(stringResource(R.string.edit_task), palette)
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                singleLine = true,
                cursorBrush = SolidColor(accent),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (trimmed.isNotEmpty()) onConfirm(trimmed) }),
                textStyle = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = palette.ink),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (title.text.isEmpty()) {
                            Text(
                                text = stringResource(R.string.task_title_hint),
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = palette.placeholder
                            )
                        }
                        inner()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(fieldShape)
                    .background(palette.background)
                    .border(1.dp, palette.border, fieldShape)
                    .padding(horizontal = 16.dp)
                    .wrapContentHeight(Alignment.CenterVertically)
                    .focusRequester(focusRequester)
            )
            DialogButtons(
                confirmText = stringResource(R.string.save),
                confirmColor = accent,
                confirmEnabled = trimmed.isNotEmpty(),
                palette = palette,
                onDismiss = onDismiss,
                onConfirm = { onConfirm(trimmed) }
            )
        }
    }
}

/** Segunda línea: fecha de vencimiento (coloreada según lo cerca que esté) o fecha de completado. */
@Composable
private fun TaskMeta(task: Task, isCompleted: Boolean, palette: HomePalette, accent: Color) {
    val isRecurring = task.recurrencia != TaskRecurrencia.NINGUNA
    val text: String?
    val color: Color
    if (isCompleted) {
        // Sin la fecha, varias ocurrencias completadas con el mismo título son indistinguibles
        text = (task.fechaCompletada ?: task.fechaVencimiento)?.let {
            stringResource(R.string.completed_on, DateUtils.formatDate(it))
        }
        color = palette.doneText
    } else {
        text = task.fechaVencimiento?.let { DateUtils.formatDate(it) }
        color = task.fechaVencimiento?.let { dueColor(DateUtils.getDueDateStatus(it), palette) } ?: palette.muted
    }
    if (text == null && !(isRecurring && !isCompleted)) return

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (text != null) {
            Text(
                text = text,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = color
            )
        }
        if (isRecurring && !isCompleted) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
        }
    }
}

internal fun priorityColor(prioridad: TaskPrioridad): Color = when (prioridad) {
    TaskPrioridad.ALTA -> PriorityHighRed
    TaskPrioridad.MEDIA -> PriorityMediumOrange
    TaskPrioridad.BAJA -> PriorityLowGreen
    TaskPrioridad.NINGUNA -> Color.Transparent
}

internal fun dueColor(status: DueDateStatus, palette: HomePalette): Color {
    val base = when (status) {
        DueDateStatus.OVERDUE -> DueOverdueRed
        DueDateStatus.DUE_TODAY -> DueTodayOrange
        DueDateStatus.DUE_TOMORROW -> DueTomorrowOrange
        DueDateStatus.DUE_SOON -> palette.brand
        DueDateStatus.NORMAL -> return palette.muted
    }
    return if (palette.isDark) lerp(base, Color.White, 0.25f) else base
}
