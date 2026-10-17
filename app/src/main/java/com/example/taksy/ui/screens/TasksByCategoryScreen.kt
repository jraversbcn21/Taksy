package com.example.taksy.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
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
import com.example.taksy.service.AlarmPolicy
import com.example.taksy.ui.components.ExactAlarmPermissionDialog
import com.example.taksy.ui.components.QuickReminderDialog
import com.example.taksy.ui.components.TaskListItem
import com.example.taksy.ui.components.tasksbycategory.InlineTaskInput
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
    var taskForReminder by remember { mutableStateOf<Task?>(null) }
    var showExactAlarmDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val displayedTasks = if (searchQuery.isNotBlank()) {
        taskViewModel.searchTasksByCategory(category.id, searchQuery).collectAsState(initial = emptyList()).value
    } else {
        tasks
    }

    val undoDeleteMessage = stringResource(R.string.task_deleted_toast)
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
                            text = CategoryUtils.getCategoryName(category),
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
                                    stringResource(R.string.no_tasks_in_category, CategoryUtils.getCategoryName(category)),
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
                    // Lista de tareas: orden fijo por estado, prioridad y fecha (ver TaskDao)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayedTasks, key = { it.id }) { task ->
                            val subtasks by taskViewModel.getSubtasksByTaskId(task.id).collectAsState(initial = emptyList())
                            val pendingSubtasksCount = subtasks.count { it.estado == TaskEstado.PENDIENTE }
                            val reminders by taskViewModel.getRemindersByTaskId(task.id).collectAsState(initial = emptyList())
                            val hasActiveReminder = reminders.any { it.activo }

                            TaskListItem(
                                task = task,
                                pendingSubtasksCount = pendingSubtasksCount,
                                isTaskCompleted = pendingSubtasksCount == 0 && subtasks.isNotEmpty(),
                                category = category,
                                hasActiveReminder = hasActiveReminder,
                                onTaskClick = onTaskClick,
                                onTaskToggle = onTaskToggle,
                                onTaskDelete = handleDeleteWithUndo,
                                onShowToast = { message -> showSnackbar(message) },
                                onReminderClick = { taskForReminder = it },
                                onEditTask = { taskViewModel.updateTask(it) },
                                modifier = Modifier.animateItem()
                            )
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
        taskForReminder?.let { reminderTask ->
            val taskReminders by taskViewModel.getRemindersByTaskId(reminderTask.id).collectAsState(initial = emptyList())
            val existingReminder = taskReminders.firstOrNull { it.activo }

            QuickReminderDialog(
                taskTitle = reminderTask.titulo,
                existingReminder = existingReminder,
                onSetReminder = { date ->
                    taskViewModel.setQuickReminder(reminderTask.id, reminderTask.titulo, date)
                    taskForReminder = null
                    // El recordatorio se guarda igual; sin el permiso solo puede llegar con retraso
                    showExactAlarmDialog = AlarmPolicy.needsExactAlarmPermission(context)
                },
                onDeleteReminder = {
                    taskViewModel.deleteReminderForTask(reminderTask.id)
                    taskForReminder = null
                },
                onDismiss = { taskForReminder = null }
            )
        }

        if (showExactAlarmDialog) {
            ExactAlarmPermissionDialog(onDismiss = { showExactAlarmDialog = false })
        }
    }
}
