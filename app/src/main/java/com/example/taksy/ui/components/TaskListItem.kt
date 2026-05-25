package com.example.taksy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.taksy.R
import com.example.taksy.data.Category
import com.example.taksy.data.Task
import com.example.taksy.data.TaskEstado
import com.example.taksy.data.TaskPrioridad
import com.example.taksy.data.TaskRecurrencia
import com.example.taksy.ui.theme.DueOverdueRed
import com.example.taksy.ui.theme.DueTodayOrange
import com.example.taksy.ui.theme.DueTomorrowOrange
import com.example.taksy.ui.theme.PriorityHighRed
import com.example.taksy.ui.theme.PriorityLowGreen
import com.example.taksy.ui.theme.PriorityMediumOrange
import com.example.taksy.ui.theme.RecurrencePurple
import com.example.taksy.ui.theme.SwipeArchiveBg
import com.example.taksy.ui.theme.SwipeDeleteBg
import com.example.taksy.utils.DateUtils
import com.example.taksy.utils.DueDateStatus

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TaskListItem(
    task: Task,
    pendingSubtasksCount: Int = 0,
    isTaskCompleted: Boolean = false,
    category: Category? = null,
    hasActiveReminder: Boolean = false,
    onTaskClick: (Task) -> Unit,
    onTaskToggle: (Task) -> Unit,
    onTaskDelete: (Task) -> Unit,
    onArchiveTask: (Task) -> Unit = {},
    onShowToast: (String) -> Unit = {},
    onReminderClick: (Task) -> Unit = {},
    onEditTask: (Task) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isToggling by remember { mutableStateOf(false) }

    LaunchedEffect(task.estado) { isToggling = false }

    val isCompleted = task.estado == TaskEstado.COMPLETADA
    val isFullyCompleted = isTaskCompleted
    val hasSubtasks = pendingSubtasksCount > 0
    val hasNoSubtasks = !hasSubtasks
    val isLocked = isCompleted && hasNoSubtasks

    val pendingSubtasksMessage = stringResource(R.string.pending_subtasks_message)

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    showDeleteDialog = true
                    false
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onArchiveTask(task)
                    true
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (direction == SwipeToDismissBoxValue.StartToEnd)
                            SwipeArchiveBg
                        else
                            SwipeDeleteBg
                    )
                    .padding(horizontal = 24.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd)
                    Alignment.CenterStart
                else
                    Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = if (direction == SwipeToDismissBoxValue.StartToEnd)
                        if (task.archivada) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
                    else
                        Icons.Default.Delete,
                    contentDescription = if (direction == SwipeToDismissBoxValue.StartToEnd)
                        stringResource(R.string.archive_task)
                    else
                        stringResource(R.string.delete_task),
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onTaskClick(task) },
                            onLongClick = {
                                editTitle = task.titulo
                                showEditDialog = true
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Radio button para marcar tarea como completada
                    RadioButton(
                        selected = isCompleted,
                        onClick = {
                            when {
                                isLocked || isToggling -> { }
                                pendingSubtasksCount > 0 -> {
                                    onShowToast(pendingSubtasksMessage)
                                }
                                else -> {
                                    isToggling = true
                                    onTaskToggle(task)
                                }
                            }
                        },
                        enabled = !isLocked && !isToggling,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary,
                            unselectedColor = MaterialTheme.colorScheme.outline,
                            disabledSelectedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            disabledUnselectedColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
                        )
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Contenido de la tarea (titulo y fecha)
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        // Titulo con indicador de prioridad
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (task.prioridad != TaskPrioridad.NINGUNA && !isFullyCompleted) {
                                val priorityColor = when (task.prioridad) {
                                    TaskPrioridad.ALTA -> PriorityHighRed
                                    TaskPrioridad.MEDIA -> PriorityMediumOrange
                                    TaskPrioridad.BAJA -> PriorityLowGreen
                                    else -> Color.Transparent
                                }
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(priorityColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = task.titulo,
                                style = MaterialTheme.typography.bodyLarge,
                                color = when {
                                    isFullyCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    isCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                textDecoration = if (isFullyCompleted) TextDecoration.LineThrough else TextDecoration.None
                            )
                        }

                        // Fecha de vencimiento (si existe y la tarea no esta completada)
                        if (!isCompleted && task.fechaVencimiento != null) {
                            val status = DateUtils.getDueDateStatus(task.fechaVencimiento)
                            val dueColor = when (status) {
                                DueDateStatus.OVERDUE -> DueOverdueRed
                                DueDateStatus.DUE_TODAY -> DueTodayOrange
                                DueDateStatus.DUE_TOMORROW -> DueTomorrowOrange
                                DueDateStatus.DUE_SOON -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                DueDateStatus.NORMAL -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = DateUtils.formatDate(task.fechaVencimiento),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = dueColor
                                )
                                if (task.recurrencia != TaskRecurrencia.NINGUNA) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = RecurrencePurple,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        if (!isCompleted && task.fechaVencimiento == null && task.recurrencia != TaskRecurrencia.NINGUNA) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = RecurrencePurple,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Categoria (si existe)
                        if (category != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            CategoryChip(category = category)
                        }
                    }

                    // Bell icon and subtask counter
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Bell icon for reminder
                        Icon(
                            imageVector = if (hasActiveReminder)
                                Icons.Default.Notifications
                            else
                                Icons.Outlined.Notifications,
                            contentDescription = stringResource(R.string.set_reminder),
                            tint = if (hasActiveReminder)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { onReminderClick(task) }
                        )

                        // Contador de subtareas pendientes
                        if (pendingSubtasksCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(
                                        when {
                                            isFullyCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                            isCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        },
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = pendingSubtasksCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when {
                                        isFullyCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        isCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        else -> MaterialTheme.colorScheme.primary
                                    },
                                    fontWeight = MaterialTheme.typography.labelSmall.fontWeight
                                )
                            }
                        }
                    }
                }

                // Linea divisoria
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_task_title)) },
            text = { Text(stringResource(R.string.delete_task_message)) },
            confirmButton = {
                TextButton(onClick = {
                    onTaskDelete(task)
                    showDeleteDialog = false
                }) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(stringResource(R.string.edit_task)) },
            text = {
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.task_title_hint),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = editTitle.trim()
                        if (trimmed.isNotBlank()) {
                            onEditTask(task.copy(titulo = trimmed))
                            showEditDialog = false
                        }
                    },
                    enabled = editTitle.isNotBlank()
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
