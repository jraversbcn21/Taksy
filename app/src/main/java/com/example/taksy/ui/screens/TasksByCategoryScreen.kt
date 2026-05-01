package com.example.taksy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.taksy.ui.components.DeleteTaskDialog
import com.example.taksy.ui.components.QuickReminderDialog
import com.example.taksy.ui.components.TaskListItem
import com.example.taksy.utils.CategoryUtils
import com.example.taksy.utils.DeviceUtils
import kotlinx.coroutines.launch

/**
 * Pantalla que muestra las tareas de una categoría específica
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksByCategoryScreen(
    category: Category,
    tasks: List<Task>,
    taskViewModel: com.example.taksy.viewmodel.TaskViewModel,
    onBackClick: () -> Unit = {},
    onTaskClick: (Task) -> Unit = {},
    onTaskToggle: (Task) -> Unit = {},
    onTaskDelete: (Task) -> Unit = {},
    onAddTask: (String, Date?) -> Unit = { _, _ -> },
    showToast: (String) -> Unit = {},
    onShowDeleteDialog: (Task) -> Unit = {}, // Nueva función para mostrar diálogo de borrado
    modifier: Modifier = Modifier
) {
    var showInlineInput by remember { mutableStateOf(false) }
    var newTaskTitle by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var taskForReminder by remember { mutableStateOf<Task?>(null) }
    val focusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val displayedTasks = if (searchQuery.isNotBlank()) {
        taskViewModel.searchTasksByCategory(category.id, searchQuery).collectAsState(initial = emptyList()).value
    } else {
        tasks
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
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
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
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
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
                    onAddTask = { title ->
                        if (title.isNotBlank()) {
                            // Añadir la tarea con fecha actual y categoría
                            val currentDate = java.util.Date()
                            onAddTask(title.trim(), currentDate)
                            newTaskTitle = "" // Limpiar el campo pero mantener el input visible
                            // No cambiar showInlineInput = false para mantener el input activo
                        }
                    },
                    onCancel = {
                        newTaskTitle = ""
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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayedTasks) { task ->
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
                                onTaskDelete = onTaskDelete,
                                onShowDeleteDialog = onShowDeleteDialog,
                                onShowToast = { message -> showSnackbar(message) },
                                onReminderClick = { taskForReminder = it }
                            )
                        }
                    }
                }
                
                // Botón flotante para añadir tarea
                FloatingActionButton(
                    onClick = { 
                        if (showInlineInput && newTaskTitle.isNotBlank()) {
                            // Si hay texto en el input, añadir la tarea
                            onAddTask(newTaskTitle.trim(), null)
                            newTaskTitle = "" // Limpiar el campo pero mantener el input visible
                        } else {
                            // Si no hay input visible o está vacío, mostrar el input
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
    onAddTask: (String) -> Unit,
    onCancel: () -> Unit,
    focusRequester: FocusRequester
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Radio button (vacío, como en la imagen)
        RadioButton(
            selected = false,
            onClick = { /* No hacer nada */ },
            enabled = false,
            colors = RadioButtonDefaults.colors(
                unselectedColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Campo de texto sin borde
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
        
        // Botón de cancelar
        if (taskTitle.isNotBlank()) {
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
            }
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
