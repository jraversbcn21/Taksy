package com.example.taksy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.taksy.R
import com.example.taksy.data.Subtask
import com.example.taksy.data.Task
import com.example.taksy.data.TaskEstado
import com.example.taksy.ui.components.Toast
import com.example.taksy.ui.components.taskdetail.InlineSubtaskInput
import com.example.taksy.ui.components.taskdetail.RecurrenceSection
import com.example.taksy.ui.components.taskdetail.SubtaskItem
import com.example.taksy.utils.DeviceUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    task: Task,
    subtasks: List<Subtask>,
    onBackClick: () -> Unit,
    onAddSubtask: (String) -> Unit,
    onToggleSubtask: (Subtask) -> Unit,
    onUpdateTask: (Task) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showInlineInput by remember { mutableStateOf(false) }
    var newSubtaskTitle by remember { mutableStateOf("") }
    var toastMessage by remember { mutableStateOf("") }
    var showToast by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val allSubtasksCompletedMessage = stringResource(R.string.all_subtasks_completed)

    val showToastMessage = { message: String ->
        toastMessage = message
        showToast = true
    }

    LaunchedEffect(showInlineInput) {
        if (showInlineInput) focusRequester.requestFocus()
    }

    val onAddSubtaskClick = {
        if (task.estado != TaskEstado.COMPLETADA) {
            if (showInlineInput && newSubtaskTitle.isNotBlank()) {
                onAddSubtask(newSubtaskTitle.trim())
                newSubtaskTitle = ""
            } else {
                showInlineInput = true
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text(
                        text = task.titulo,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item(key = "recurrence_section") {
                    RecurrenceSection(
                        task = task,
                        onRecurrenceChange = { recurrence ->
                            onUpdateTask(task.copy(recurrencia = recurrence))
                        }
                    )
                }

                item(key = "subtasks_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.subtasks_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onAddSubtaskClick) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.add_subtask_description),
                                tint = if (task.estado == TaskEstado.COMPLETADA)
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (showInlineInput) {
                    item(key = "inline_subtask_input") {
                        InlineSubtaskInput(
                            subtaskTitle = newSubtaskTitle,
                            onSubtaskTitleChange = { newSubtaskTitle = it },
                            onAddSubtask = { title ->
                                if (title.isNotBlank()) {
                                    onAddSubtask(title.trim())
                                    newSubtaskTitle = ""
                                }
                            },
                            onCancel = {
                                newSubtaskTitle = ""
                                showInlineInput = false
                            },
                            focusRequester = focusRequester
                        )
                    }
                }

                if (subtasks.isEmpty()) {
                    item(key = "subtasks_empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.all_reminders_completed),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    val sortedSubtasks = subtasks.sortedWith { a, b ->
                        when {
                            a.estado == TaskEstado.PENDIENTE && b.estado == TaskEstado.COMPLETADA -> -1
                            a.estado == TaskEstado.COMPLETADA && b.estado == TaskEstado.PENDIENTE -> 1
                            else -> 0
                        }
                    }

                    items(
                        items = sortedSubtasks,
                        key = { subtask -> "subtask_${subtask.id}" }
                    ) { subtask ->
                        SubtaskItem(
                            subtask = subtask,
                            onToggle = {
                                if (task.estado != TaskEstado.COMPLETADA) {
                                    val wasPending = subtask.estado == TaskEstado.PENDIENTE
                                    val pendingBefore = sortedSubtasks.count { it.estado == TaskEstado.PENDIENTE }
                                    onToggleSubtask(subtask)
                                    if (wasPending && pendingBefore == 1) {
                                        showToastMessage(allSubtasksCompletedMessage)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                FloatingActionButton(
                    onClick = onAddSubtaskClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = DeviceUtils.getFabSidePadding(),
                            bottom = DeviceUtils.getSubtaskFabBottomPadding()
                        ),
                    containerColor = if (task.estado == TaskEstado.COMPLETADA)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.primary,
                    contentColor = if (task.estado == TaskEstado.COMPLETADA)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_subtask_description)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1000f),
            contentAlignment = Alignment.BottomCenter
        ) {
            Toast(
                message = toastMessage,
                isVisible = showToast,
                onDismiss = { showToast = false }
            )
        }
    }
}
