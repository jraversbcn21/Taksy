package com.example.taksy.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.taksy.ui.theme.HomePalette
import com.example.taksy.ui.theme.PlusJakartaSans
import com.example.taksy.ui.theme.accentColor
import com.example.taksy.utils.CategoryUtils
import com.example.taksy.utils.DeviceUtils
import com.example.taksy.utils.getCategoryIcon
import kotlinx.coroutines.launch
import java.util.Date

/**
 * Tareas de una categoría, con el lenguaje visual de la pantalla de inicio: cabecera con el
 * icono y el progreso de la lista, tarjeta para añadir tareas y secciones de pendientes y completadas.
 */
@Composable
fun TasksByCategoryScreen(
    category: Category,
    tasks: List<Task>,
    taskViewModel: com.example.taksy.viewmodel.TaskViewModel,
    isDarkMode: Boolean = false,
    onBackClick: () -> Unit = {},
    onTaskClick: (Task) -> Unit = {},
    onTaskToggle: (Task) -> Unit = {},
    onAddTask: (String, Date?, TaskPrioridad, TaskRecurrencia) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val palette = if (isDarkMode) HomePalette.Dark else HomePalette.Light
    val accent = remember(category.color) { category.accentColor() }

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
    val listState = rememberLazyListState()

    val displayedTasks = if (searchQuery.isNotBlank()) {
        taskViewModel.searchTasksByCategory(category.id, searchQuery).collectAsState(initial = emptyList()).value
    } else {
        tasks
    }
    val pendingTasks = displayedTasks.filter { it.estado == TaskEstado.PENDIENTE }
    val completedTasks = displayedTasks.filter { it.estado == TaskEstado.COMPLETADA }

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
    }

    fun resetComposer() {
        newTaskTitle = ""
        selectedPriority = TaskPrioridad.NINGUNA
        selectedRecurrence = TaskRecurrencia.NINGUNA
    }

    fun openComposer() {
        showInlineInput = true
        coroutineScope.launch { listState.animateScrollToItem(0) }
    }

    // Enfocar automáticamente el campo cuando se muestra
    LaunchedEffect(showInlineInput) {
        if (showInlineInput) focusRequester.requestFocus()
    }

    @Composable
    fun TaskRow(task: Task, modifier: Modifier) {
        val subtasks by taskViewModel.getSubtasksByTaskId(task.id).collectAsState(initial = emptyList())
        val reminders by taskViewModel.getRemindersByTaskId(task.id).collectAsState(initial = emptyList())
        TaskListItem(
            task = task,
            palette = palette,
            accent = accent,
            pendingSubtasksCount = subtasks.count { it.estado == TaskEstado.PENDIENTE },
            hasActiveReminder = reminders.any { it.activo },
            onTaskClick = onTaskClick,
            onTaskToggle = onTaskToggle,
            onTaskDelete = handleDeleteWithUndo,
            onShowToast = showSnackbar,
            onReminderClick = { taskForReminder = it },
            onEditTask = { taskViewModel.updateTask(it) },
            modifier = modifier
        )
    }

    Scaffold(
        containerColor = palette.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!showInlineInput && !showSearch) {
                FloatingActionButton(
                    onClick = ::openComposer,
                    shape = RoundedCornerShape(18.dp),
                    containerColor = accent,
                    contentColor = Color.White,
                    modifier = Modifier.padding(
                        end = DeviceUtils.getFabSidePadding() - 16.dp,
                        bottom = DeviceUtils.getFabBottomPadding() - 16.dp
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_task))
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item(key = "top_bar") {
                if (showSearch) {
                    SearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onClose = {
                            showSearch = false
                            searchQuery = ""
                        },
                        palette = palette,
                        accent = accent
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = palette.ink)
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { showSearch = true }) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search_tasks), tint = palette.ink)
                        }
                    }
                }
            }

            if (!showSearch) {
                item(key = "header") {
                    CategoryHeader(
                        category = category,
                        accent = accent,
                        doneCount = tasks.count { it.estado == TaskEstado.COMPLETADA },
                        totalCount = tasks.size,
                        palette = palette,
                        modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 12.dp)
                    )
                }
                item(key = "composer") {
                    if (showInlineInput) {
                        InlineTaskInput(
                            taskTitle = newTaskTitle,
                            onTaskTitleChange = { newTaskTitle = it },
                            selectedPriority = selectedPriority,
                            onPriorityChange = { selectedPriority = it },
                            selectedRecurrence = selectedRecurrence,
                            onRecurrenceChange = { selectedRecurrence = it },
                            onAddTask = { title ->
                                // Sin fecha: se pone en el detalle o con el recordatorio
                                onAddTask(title, null, selectedPriority, selectedRecurrence)
                                resetComposer()
                            },
                            onCancel = {
                                resetComposer()
                                showInlineInput = false
                            },
                            focusRequester = focusRequester,
                            palette = palette,
                            accent = accent
                        )
                    } else {
                        AddTaskPill(palette = palette, accent = accent, onClick = ::openComposer)
                    }
                }
            }

            if (displayedTasks.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        title = if (searchQuery.isNotBlank()) stringResource(R.string.no_search_results)
                        else stringResource(R.string.no_tasks_in_category, CategoryUtils.getCategoryName(category)),
                        subtitle = if (searchQuery.isBlank() && !showInlineInput) stringResource(R.string.add_first_task_to_category) else null,
                        palette = palette
                    )
                }
            }

            if (pendingTasks.isNotEmpty()) {
                item(key = "pending_label") {
                    SectionLabel(stringResource(R.string.tasks_section_pending), pendingTasks.size, palette)
                }
                // Orden fijo por estado, prioridad y fecha (ver TaskDao)
                items(pendingTasks, key = { it.id }) { task -> TaskRow(task, Modifier.animateItem()) }
            }
            if (completedTasks.isNotEmpty()) {
                item(key = "done_label") {
                    SectionLabel(stringResource(R.string.tasks_section_done), completedTasks.size, palette)
                }
                items(completedTasks, key = { it.id }) { task -> TaskRow(task, Modifier.animateItem()) }
            }
        }

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

@Composable
private fun CategoryHeader(
    category: Category,
    accent: Color,
    doneCount: Int,
    totalCount: Int,
    palette: HomePalette,
    modifier: Modifier = Modifier
) {
    val target = if (totalCount > 0) doneCount.toFloat() / totalCount else 0f
    val progress by animateFloatAsState(target, tween(450), label = "progress")

    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(accent)
            ) {
                Icon(getCategoryIcon(category.icono), contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
            Text(
                text = CategoryUtils.getCategoryName(category),
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                lineHeight = 36.sp,
                letterSpacing = (-1).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = palette.ink
            )
        }
        if (totalCount > 0) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    text = stringResource(R.string.home_done_of, doneCount, totalCount),
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = palette.muted
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
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
}

@Composable
private fun AddTaskPill(palette: HomePalette, accent: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(palette.surface)
            .border(1.dp, palette.border, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accent.copy(alpha = if (palette.isDark) 0.22f else 0.14f))
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        }
        Text(
            text = stringResource(R.string.add_task),
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = palette.placeholder
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    palette: HomePalette,
    accent: Color
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val shape = RoundedCornerShape(16.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(50.dp)
            .clip(shape)
            .background(palette.surface)
            .border(1.dp, palette.border, shape)
            .padding(start = 16.dp)
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = palette.placeholder, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            cursorBrush = SolidColor(accent),
            textStyle = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = palette.ink),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.search_tasks),
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = palette.placeholder
                    )
                }
                inner()
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
        )
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = palette.muted)
        }
    }
}

@Composable
private fun SectionLabel(text: String, count: Int, palette: HomePalette) {
    Text(
        text = "${text.uppercase()} · $count",
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 13.sp,
        letterSpacing = 1.sp,
        color = palette.muted,
        modifier = Modifier.padding(start = 6.dp, top = 16.dp, bottom = 2.dp)
    )
}

@Composable
private fun EmptyState(title: String, subtitle: String?, palette: HomePalette) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp)
    ) {
        Text(
            text = title,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            color = palette.ink
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = palette.muted
            )
        }
    }
}
