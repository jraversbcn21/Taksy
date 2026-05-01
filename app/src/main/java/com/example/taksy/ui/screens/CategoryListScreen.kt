package com.example.taksy.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.taksy.R
import com.example.taksy.data.Category
import com.example.taksy.data.Task
import com.example.taksy.data.TaskEstado
import com.example.taksy.data.TaskPrioridad
import com.example.taksy.ui.components.CategoryItem
import com.example.taksy.ui.components.HamburgerMenu
import com.example.taksy.ui.components.Toast
import com.example.taksy.utils.DateUtils
import com.example.taksy.utils.DueDateStatus
import com.example.taksy.utils.DeviceUtils

/**
 * Pantalla principal que muestra las categorias como elementos clicables
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CategoryListScreen(
    categories: List<Category>,
    taskCountByCategory: Map<Long, Int> = emptyMap(),
    isDarkMode: Boolean = false,
    currentLanguage: String = "es",
    onSettingsClick: () -> Unit = {},
    onCategoryClick: (Category) -> Unit = {},
    onEditCategoryClick: (Category) -> Unit = {},
    onDeleteCategoryClick: (Category) -> Unit = {},
    onReorderCategories: (List<Category>) -> Unit = {},
    showToast: (String) -> Unit = {},
    searchResults: List<Task> = emptyList(),
    onSearchQueryChanged: (String) -> Unit = {},
    onTaskClick: (Task) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var toastMessage by remember { mutableStateOf("") }
    var showToastState by remember { mutableStateOf(false) }

    // Estado para drag & drop
    var categoriesState by remember { mutableStateOf(categories) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var draggingIndex by remember { mutableStateOf(-1) }
    var accumulatedDelta by remember { mutableStateOf(0f) }

    // Estado de busqueda
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Actualizar el estado cuando cambien las categorias
    LaunchedEffect(categories) {
        categoriesState = categories
    }

    // Mapa de categorias para busqueda rapida por id
    val categoryMap = remember(categories) {
        categories.associateBy { it.id }
    }

    // Funcion para reordenar categorias
    fun moveCategory(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return

        val newCategories = categoriesState.toMutableList()
        val item = newCategories.removeAt(fromIndex)
        newCategories.add(toIndex, item)

        val reorderedCategories = newCategories.mapIndexed { index, category ->
            category.copy(orden = index + 1)
        }

        categoriesState = reorderedCategories
        onReorderCategories(reorderedCategories)
    }

    Scaffold(
        topBar = {
            if (showSearch) {
                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                onSearchQueryChanged(it)
                            },
                            placeholder = { Text(stringResource(R.string.search_tasks)) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            showSearch = false
                            searchQuery = ""
                            onSearchQueryChanged("")
                        }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.app_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    },
                    navigationIcon = {
                        HamburgerMenu(
                            isDarkMode = isDarkMode,
                            currentLanguage = currentLanguage,
                            onSettingsClick = onSettingsClick
                        )
                    },
                    actions = {
                        IconButton(onClick = { showSearch = true }) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = stringResource(R.string.search_tasks)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
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
            if (showSearch && searchQuery.isNotBlank()) {
                // Mostrar resultados de busqueda
                if (searchResults.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_search_results),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        items(searchResults, key = { it.id }) { task ->
                            SearchResultItem(
                                task = task,
                                category = task.categoriaId?.let { categoryMap[it] },
                                onClick = { onTaskClick(task) }
                            )
                        }
                    }
                }
            } else {
                // Lista de categorias con drag & drop
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        end = 8.dp,
                        top = 8.dp,
                        bottom = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    flingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(listState)
                ) {
                    items(
                        items = categoriesState,
                        key = { it.id },
                        itemContent = { category ->
                            val index = categoriesState.indexOf(category)
                            CategoryItem(
                                category = category,
                                taskCount = taskCountByCategory[category.id] ?: 0,
                                onClick = { onCategoryClick(category) },
                                isDragging = draggingIndex == index,
                                modifier = Modifier
                                    .animateItem()
                                    .draggable(
                                        state = rememberDraggableState { delta ->
                                            accumulatedDelta += delta

                                            val minThreshold = 50f
                                            if (kotlin.math.abs(accumulatedDelta) < minThreshold) {
                                                return@rememberDraggableState
                                            }

                                            val thresholdUp = 300f
                                            val thresholdDown = 350f
                                            val threshold = if (accumulatedDelta < 0) thresholdUp else thresholdDown
                                            val positionChange = (accumulatedDelta / threshold).toInt()
                                            val actualPositionChange = positionChange.coerceIn(-1, 1)

                                            if (actualPositionChange != 0) {
                                                val newIndex = (index + actualPositionChange).coerceIn(0, categoriesState.size - 1)
                                                if (newIndex != index) {
                                                    scope.launch {
                                                        moveCategory(index, newIndex)
                                                    }
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
                            )
                        }
                    )
                }
            }
        }

        // Toast para mostrar mensajes
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Toast(
                message = toastMessage,
                isVisible = showToastState,
                onDismiss = { showToastState = false },
                backgroundColor = Color(0xFFE57373).copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun SearchResultItem(
    task: Task,
    category: Category?,
    onClick: () -> Unit
) {
    val isCompleted = task.estado == TaskEstado.COMPLETADA

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (task.prioridad != TaskPrioridad.NINGUNA && !isCompleted) {
                        val priorityColor = when (task.prioridad) {
                            TaskPrioridad.ALTA -> Color(0xFFE53935)
                            TaskPrioridad.MEDIA -> Color(0xFFFF9800)
                            TaskPrioridad.BAJA -> Color(0xFF4CAF50)
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
                        color = if (isCompleted)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                }

                // Fecha de vencimiento
                if (!isCompleted && task.fechaVencimiento != null) {
                    val status = DateUtils.getDueDateStatus(task.fechaVencimiento)
                    val dueColor = when (status) {
                        DueDateStatus.OVERDUE -> Color(0xFFE53935)
                        DueDateStatus.DUE_TODAY -> Color(0xFFFF6F00)
                        DueDateStatus.DUE_TOMORROW -> Color(0xFFF57C00)
                        DueDateStatus.DUE_SOON -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        DueDateStatus.NORMAL -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    }
                    Text(
                        text = DateUtils.formatDate(task.fechaVencimiento),
                        style = MaterialTheme.typography.bodySmall,
                        color = dueColor
                    )
                }

                // Nombre de la categoria
                if (category != null) {
                    Text(
                        text = category.nombre,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
