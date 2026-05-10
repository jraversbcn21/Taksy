package com.example.taksy.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import java.util.Date
import com.example.taksy.R
import com.example.taksy.data.Category
import com.example.taksy.data.Task
import com.example.taksy.data.TaskEstado
import com.example.taksy.data.TaskPrioridad
import com.example.taksy.data.TaskRecurrencia
import com.example.taksy.ui.components.QuickReminderDialog
import com.example.taksy.ui.components.TaskListItem
import com.example.taksy.ui.theme.PriorityHighRed
import com.example.taksy.ui.theme.PriorityLowGreen
import com.example.taksy.ui.theme.PriorityMediumOrange
import com.example.taksy.ui.theme.RecurrencePurple
import com.example.taksy.utils.CategoryUtils
import com.example.taksy.utils.DeviceUtils
import kotlinx.coroutines.launch

/**
 * Pantalla que muestra las tareas de una categoría específica
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TasksByCategoryScreen(
    category: Category,
    tasks: List<Task>,
    taskViewModel: com.example.taksy.viewmodel.TaskViewModel,
    onBackClick: () -> Unit = {},
    onTaskClick: (Task) -> Unit = {},
    onTaskToggle: (Task) -> Unit = {},
    onTaskDelete: (Task) -> Unit = {},
    onAddTask: (String, Date?, TaskPrioridad, com.example.taksy.data.TaskRecurrencia) -> Unit = { _, _, _, _ -> },
    showToast: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showInlineInput by remember { mutableStateOf(false) }
    var newTaskTitle by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(TaskPrioridad.NINGUNA) }
    var selectedRecurrence by remember { mutableStateOf(TaskRecurrencia.NINGUNA) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var showArchived by remember { mutableStateOf(false) }
    var taskForReminder by remember { mutableStateOf<Task?>(null) }
    val focusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Drag & drop state
    var tasksState by remember { mutableStateOf(tasks) }
    val listState = rememberLazyListState()
    var draggingIndex by remember { mutableStateOf(-1) }
    var accumulatedDelta by remember { mutableStateOf(0f) }

    LaunchedEffect(tasks) {
        tasksState = tasks
    }

    fun moveTask(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val newTasks = tasksState.toMutableList()
        val item = newTasks.removeAt(fromIndex)
        newTasks.add(toIndex, item)
        val reordered = newTasks.mapIndexed { index, task ->
            task.copy(orden = index + 1)
        }
        tasksState = reordered
        taskViewModel.reorderTasks(reordered)
    }

    val archivedTasks by taskViewModel.getArchivedTasksByCategory(category.id).collectAsState(initial = emptyList())

    val displayedTasks = if (searchQuery.isNotBlank()) {
        taskViewModel.searchTasksByCategory(category.id, searchQuery).collectAsState(initial = emptyList()).value
    } else {
        tasks
    }

    val undoDeleteMessage = stringResource(R.string.task_deleted_toast)
    val taskArchivedMessage = stringResource(R.string.task_archived)
    val taskUnarchivedMessage = stringResource(R.string.task_unarchived)
    val undoActionLabel = stringResource(R.string.undo)

    val handleDeleteWithUndo: (Task) -> Unit = { deletedTask ->
        taskViewModel.deleteTask(deletedTask)
        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = undoDeleteMessage,
                actionLabel = undoActionLabel,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                taskViewModel.restoreTask(deletedTask)
            }
        }
    }

    val handleArchiveWithUndo: (Task) -> Unit = { task ->
        if (task.archivada) {
            taskViewModel.unarchiveTask(task)
            coroutineScope.launch {
                snackbarHostState.showSnackbar(taskUnarchivedMessage, duration = SnackbarDuration.Short)
            }
        } else {
            taskViewModel.archiveTask(task)
            coroutineScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = taskArchivedMessage,
                    actionLabel = undoActionLabel,
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    taskViewModel.unarchiveTask(task)
                }
            }
        }
    }

    val showSnackbar: (String) -> Unit = { message ->
        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
        showToast(message)
    }

    // Enfocar automáticamente el campo cuando se muestra
    LaunchedEffect(showInlineInput) {
        if (showInlineInput) {
            focusRequester.requestFocus()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (showSearch) {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(stringResource(R.string.search_tasks)) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            showSearch = false
                            searchQuery = ""
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = CategoryUtils.getCategoryName(category.icono),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    actions = {
                        if (archivedTasks.isNotEmpty()) {
                            IconButton(onClick = { showArchived = !showArchived }) {
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = stringResource(
                                        if (showArchived) R.string.hide_archived else R.string.show_archived
                                    ),
                                    tint = if (showArchived) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                        IconButton(onClick = { showSearch = true }) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search_tasks))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Input inline para nueva tarea
            if (showInlineInput) {
                InlineTaskInput(
                    taskTitle = newTaskTitle,
                    onTaskTitleChange = { newTaskTitle = it },
                    selectedPriority = selectedPriority,
                    onPriorityChange = { selectedPriority = it },
                    selectedRecurrence = selectedRecurrence,
                    onRecurrenceChange = { selectedRecurrence = it },
                    onAddTask = { title ->
                        if (title.isNotBlank()) {
                            val currentDate = java.util.Date()
                            onAddTask(title.trim(), currentDate, selectedPriority, selectedRecurrence)
                            newTaskTitle = ""
                            selectedPriority = TaskPrioridad.NINGUNA
                            selectedRecurrence = TaskRecurrencia.NINGUNA
                        }
                    },
                    onCancel = {
                        newTaskTitle = ""
                        selectedPriority = TaskPrioridad.NINGUNA
                        selectedRecurrence = TaskRecurrencia.NINGUNA
                        showInlineInput = false
                    },
                    focusRequester = focusRequester
                )
            }
            
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                if (displayedTasks.isEmpty()) {
                    // Mensaje cuando no hay tareas en esta categoría
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank())
                                    stringResource(R.string.no_search_results)
                                else
                                    stringResource(R.string.no_tasks_in_category, CategoryUtils.getCategoryName(category.icono)),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (searchQuery.isBlank()) {
                                Text(
                                    text = stringResource(R.string.add_first_task_to_category),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(onClick = { showInlineInput = true }) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.add_task))
                                }
                            }
                        }
                    }
                } else {
                    // Lista de tareas
                    val isDraggable = searchQuery.isBlank()
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = if (isDraggable) tasksState else displayedTasks,
                            key = { it.id }
                        ) { task ->
                            val index = tasksState.indexOf(task)
                            val subtasks by taskViewModel.getSubtasksByTaskId(task.id).collectAsState(initial = emptyList())
                            val pendingSubtasksCount = subtasks.count { it.estado == TaskEstado.PENDIENTE }
                            val reminders by taskViewModel.getRemindersByTaskId(task.id).collectAsState(initial = emptyList())
                            val hasActiveReminder = reminders.any { it.activo }

                            val dragModifier = if (isDraggable) {
                                Modifier
                                    .animateItem()
                                    .draggable(
                                        state = rememberDraggableState { delta ->
                                            accumulatedDelta += delta
                                            val minThreshold = 50f
                                            if (kotlin.math.abs(accumulatedDelta) < minThreshold) return@rememberDraggableState
                                            val thresholdUp = 300f
                                            val thresholdDown = 350f
                                            val threshold = if (accumulatedDelta < 0) thresholdUp else thresholdDown
                                            val positionChange = (accumulatedDelta / threshold).toInt().coerceIn(-1, 1)
                                            if (positionChange != 0) {
                                                val newIndex = (index + positionChange).coerceIn(0, tasksState.size - 1)
                                                if (newIndex != index) {
                                                    coroutineScope.launch { moveTask(index, newIndex) }
                                                    accumulatedDelta = 0f
                                                }
                                            }
                                        },
                                        orientation = androidx.compose.foundation.gestures.Orientation.Vertical,
                                        onDragStarted = {
                                            draggingIndex = index
                                            accumulatedDelta = 0f
                                        },
                                        onDragStopped = {
                                            draggingIndex = -1
                                            accumulatedDelta = 0f
                                        }
                                    )
                            } else Modifier

                            TaskListItem(
                                task = task,
                                pendingSubtasksCount = pendingSubtasksCount,
                                isTaskCompleted = pendingSubtasksCount == 0 && subtasks.isNotEmpty(),
                                category = category,
                                hasActiveReminder = hasActiveReminder,
                                onTaskClick = onTaskClick,
                                onTaskToggle = onTaskToggle,
                                onTaskDelete = handleDeleteWithUndo,
                                onArchiveTask = handleArchiveWithUndo,
                                onShowToast = { message -> showSnackbar(message) },
                                onReminderClick = { taskForReminder = it },
                                modifier = dragModifier
                            )
                        }

                        if (showArchived && archivedTasks.isNotEmpty()) {
                            item(key = "archived_header") {
                                Text(
                                    text = stringResource(R.string.archived_section),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                )
                            }
                            items(archivedTasks, key = { "archived_${it.id}" }) { task ->
                                TaskListItem(
                                    task = task,
                                    category = category,
                                    onTaskClick = onTaskClick,
                                    onTaskToggle = {},
                                    onTaskDelete = handleDeleteWithUndo,
                                    onArchiveTask = handleArchiveWithUndo,
                                    onShowToast = { message -> showSnackbar(message) }
                                )
                            }
                        }
                    }
                }
                
                // Botón flotante para añadir tarea
                FloatingActionButton(
                    onClick = {
                        if (showInlineInput && newTaskTitle.isNotBlank()) {
                            onAddTask(newTaskTitle.trim(), null, selectedPriority, selectedRecurrence)
                            newTaskTitle = ""
                            selectedPriority = TaskPrioridad.NINGUNA
                            selectedRecurrence = TaskRecurrencia.NINGUNA
                        } else {
                            showInlineInput = true
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = DeviceUtils.getFabSidePadding(),
                            bottom = DeviceUtils.getFabBottomPadding()
                        ),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_task)
                    )
                }
            }
        }

        // Quick reminder dialog
        if (taskForReminder != null) {
            val reminderTask = taskForReminder!!
            val taskReminders by taskViewModel.getRemindersByTaskId(reminderTask.id).collectAsState(initial = emptyList())
            val existingReminder = taskReminders.firstOrNull { it.activo }

            QuickReminderDialog(
                taskTitle = reminderTask.titulo,
                existingReminder = existingReminder,
                onSetReminder = { date ->
                    taskViewModel.setQuickReminder(reminderTask.id, reminderTask.titulo, date)
                    taskForReminder = null
                },
                onDeleteReminder = {
                    taskViewModel.deleteReminderForTask(reminderTask.id)
                    taskForReminder = null
                },
                onDismiss = { taskForReminder = null }
            )
        }
    }
}

@Composable
private fun InlineTaskInput(
    taskTitle: String,
    onTaskTitleChange: (String) -> Unit,
    selectedPriority: TaskPrioridad,
    onPriorityChange: (TaskPrioridad) -> Unit,
    selectedRecurrence: TaskRecurrencia,
    onRecurrenceChange: (TaskRecurrencia) -> Unit,
    onAddTask: (String) -> Unit,
    onCancel: () -> Unit,
    focusRequester: FocusRequester
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = false,
                onClick = { },
                enabled = false,
                colors = RadioButtonDefaults.colors(
                    unselectedColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            Spacer(modifier = Modifier.width(12.dp))

            BasicTextField(
                value = taskTitle,
                onValueChange = onTaskTitleChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (taskTitle.isNotBlank()) {
                            onAddTask(taskTitle.trim())
                        }
                    }
                ),
                decorationBox = { innerTextField ->
                    if (taskTitle.isEmpty()) {
                        Text(
                            text = stringResource(R.string.add_task),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    innerTextField()
                }
            )

            if (taskTitle.isNotBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }

        // Priority selector row
        PrioritySelector(
            selectedPriority = selectedPriority,
            onPriorityChange = onPriorityChange,
            modifier = Modifier.padding(start = 48.dp, top = 4.dp)
        )

        // Recurrence selector row
        RecurrenceSelector(
            selectedRecurrence = selectedRecurrence,
            onRecurrenceChange = onRecurrenceChange,
            modifier = Modifier.padding(start = 48.dp, top = 4.dp)
        )
    }
}

@Composable
private fun PrioritySelector(
    selectedPriority: TaskPrioridad,
    onPriorityChange: (TaskPrioridad) -> Unit,
    modifier: Modifier = Modifier
) {
    val priorities = listOf(
        TaskPrioridad.NINGUNA to Triple(R.string.priority_none, Color.Gray, MaterialTheme.colorScheme.outline),
        TaskPrioridad.BAJA to Triple(R.string.priority_low, PriorityLowGreen, PriorityLowGreen),
        TaskPrioridad.MEDIA to Triple(R.string.priority_medium, PriorityMediumOrange, PriorityMediumOrange),
        TaskPrioridad.ALTA to Triple(R.string.priority_high, PriorityHighRed, PriorityHighRed)
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        priorities.forEach { (priority, triple) ->
            val (labelRes, _, accentColor) = triple
            val isSelected = selectedPriority == priority

            FilterChip(
                selected = isSelected,
                onClick = { onPriorityChange(priority) },
                label = {
                    Text(
                        text = stringResource(labelRes),
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accentColor.copy(alpha = 0.15f),
                    selectedLabelColor = accentColor
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    selectedBorderColor = accentColor
                ),
                modifier = Modifier.height(28.dp)
            )
        }
    }
}

@Composable
private fun RecurrenceSelector(
    selectedRecurrence: TaskRecurrencia,
    onRecurrenceChange: (TaskRecurrencia) -> Unit,
    modifier: Modifier = Modifier
) {
    val recurrenceColor = RecurrencePurple
    val recurrences = listOf(
        TaskRecurrencia.NINGUNA to R.string.recurrence_none,
        TaskRecurrencia.DIARIA to R.string.recurrence_daily,
        TaskRecurrencia.SEMANAL to R.string.recurrence_weekly,
        TaskRecurrencia.MENSUAL to R.string.recurrence_monthly,
        TaskRecurrencia.ANUAL to R.string.recurrence_yearly
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        recurrences.forEach { (recurrence, labelRes) ->
            val isSelected = selectedRecurrence == recurrence
            val chipColor = if (recurrence == TaskRecurrencia.NINGUNA) MaterialTheme.colorScheme.outline else recurrenceColor

            FilterChip(
                selected = isSelected,
                onClick = { onRecurrenceChange(recurrence) },
                label = {
                    Text(
                        text = stringResource(labelRes),
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = chipColor.copy(alpha = 0.15f),
                    selectedLabelColor = chipColor
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    selectedBorderColor = chipColor
                ),
                modifier = Modifier.height(28.dp)
            )
        }
    }
}

@Composable
private fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Date?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.new_task))
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.task_title_hint),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Usar OutlinedTextField con colores explícitos
                OutlinedTextField(
                    value = title,
                        onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.task_title_hint),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        cursorColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    if (title.isNotBlank()) {
                        onConfirm(title.trim(), null)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text(stringResource(R.string.add_task))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
