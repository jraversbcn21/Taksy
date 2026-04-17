package com.example.taksy.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.taksy.R
import com.example.taksy.data.Category
import com.example.taksy.ui.components.CategoryItem
import com.example.taksy.ui.components.HamburgerMenu
import com.example.taksy.ui.components.Toast
import com.example.taksy.utils.DeviceUtils

/**
 * Pantalla principal que muestra las categorías como elementos clicables
 * Reemplaza la funcionalidad de TaskListScreen
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
    
    // Actualizar el estado cuando cambien las categorías
    LaunchedEffect(categories) {
        categoriesState = categories
    }
    
    // Función para reordenar categorías
    fun moveCategory(fromIndex: Int, toIndex: Int) {
        android.util.Log.d("CategoryListScreen", "moveCategory: fromIndex=$fromIndex, toIndex=$toIndex")
        if (fromIndex == toIndex) {
            android.util.Log.d("CategoryListScreen", "moveCategory: same index, returning")
            return
        }
        
        val newCategories = categoriesState.toMutableList()
        val item = newCategories.removeAt(fromIndex)
        newCategories.add(toIndex, item)
        
        android.util.Log.d("CategoryListScreen", "moveCategory: moved item ${item.nombre} from $fromIndex to $toIndex")
        
        // Actualizar el orden de las categorías
        val reorderedCategories = newCategories.mapIndexed { index, category ->
            category.copy(orden = index + 1)
        }
        
        categoriesState = reorderedCategories
        onReorderCategories(reorderedCategories)
        android.util.Log.d("CategoryListScreen", "moveCategory: categories reordered and saved")
    }
    
    Scaffold(
        topBar = {
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Lista de categorías con drag & drop
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
                                .animateItemPlacement()
                                .draggable(
                                    state = rememberDraggableState { delta ->
                                        android.util.Log.d("CategoryListScreen", "draggable: delta=$delta, index=$index, accumulatedDelta=$accumulatedDelta")
                                        
                                        // Acumular el delta
                                        accumulatedDelta += delta
                                        
                                        // Solo procesar si hay suficiente movimiento acumulado (umbral mínimo)
                                        val minThreshold = 50f
                                        if (kotlin.math.abs(accumulatedDelta) < minThreshold) {
                                            return@rememberDraggableState
                                        }
                                        
                                        // Calcular nueva posición basada en el delta acumulado
                                        // Umbrales más altos para hacer el drag & drop más pesado (menos sensible)
                                        val thresholdUp = 300f    // Para subir (delta negativo) - más pesado
                                        val thresholdDown = 350f  // Para bajar (delta positivo) - más pesado
                                        val threshold = if (accumulatedDelta < 0) thresholdUp else thresholdDown
                                        val positionChange = (accumulatedDelta / threshold).toInt()
                                        
                                        // Solo permitir cambios de 1 posición a la vez
                                        val actualPositionChange = positionChange.coerceIn(-1, 1)
                                        
                                        if (actualPositionChange != 0) {
                                            val newIndex = (index + actualPositionChange).coerceIn(0, categoriesState.size - 1)
                                            android.util.Log.d("CategoryListScreen", "draggable: actualPositionChange=$actualPositionChange, calculated newIndex=$newIndex")
                                            
                                            if (newIndex != index) {
                                                android.util.Log.d("CategoryListScreen", "draggable: moving from $index to $newIndex")
                                                scope.launch {
                                                    moveCategory(index, newIndex)
                                                }
                                                // Resetear el delta acumulado después del movimiento
                                                accumulatedDelta = 0f
                                            }
                                        }
                                    },
                                    orientation = androidx.compose.foundation.gestures.Orientation.Vertical,
                                    onDragStarted = { 
                                        android.util.Log.d("CategoryListScreen", "onDragStarted: category=${category.nombre}")
                                        draggingIndex = index
                                        accumulatedDelta = 0f
                                    },
                                    onDragStopped = { 
                                        android.util.Log.d("CategoryListScreen", "onDragStopped: category=${category.nombre}")
                                        draggingIndex = -1
                                        accumulatedDelta = 0f
                                    }
                                )
                        )
                    }
                )
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
