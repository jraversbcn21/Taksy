package com.example.taksy.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.taksy.R
import com.example.taksy.data.Category
import com.example.taksy.data.Task
import com.example.taksy.data.TaskEstado
import com.example.taksy.utils.DateUtils
import com.example.taksy.utils.DueDateStatus
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun TaskListItem(
    task: Task,
    pendingSubtasksCount: Int = 0,
    isTaskCompleted: Boolean = false, // Nueva propiedad para indicar si la tarea está completamente terminada
    category: Category? = null, // Categoría de la tarea
    onTaskClick: (Task) -> Unit,
    onTaskToggle: (Task) -> Unit,
    onTaskDelete: (Task) -> Unit,
    onShowDeleteDialog: (Task) -> Unit = {}, // Nueva función para mostrar diálogo de borrado
    onShowToast: (String) -> Unit = {},
    onNavigateToSubtasks: (Task) -> Unit = {}, // Nueva función para navegar a subtareas
    modifier: Modifier = Modifier
) {
    var offsetX by remember(task.id) { mutableStateOf(0f) }
    var isDeleting by remember(task.id) { mutableStateOf(false) }
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    
    val isCompleted = task.estado == TaskEstado.COMPLETADA
    val isFullyCompleted = isTaskCompleted // Tarea completamente terminada (todas las subtareas completadas)
    val hasSubtasks = pendingSubtasksCount > 0 // Tiene subtareas solo si hay subtareas pendientes (si hay contador visible)
    val hasNoSubtasks = !hasSubtasks // Tarea sin subtareas (no hay contador visible)
    val isLocked = isCompleted && hasNoSubtasks // Tarea bloqueada solo si está completada Y no tiene subtareas
    
    // Obtener el mensaje de toast una sola vez
    val pendingSubtasksMessage = stringResource(R.string.pending_subtasks_message)
    
    val deleteAlpha by animateFloatAsState(
        targetValue = if (isDeleting) 0f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
    )
    
    val animatedOffsetX by animateFloatAsState(
        targetValue = if (isDeleting) -1000f else offsetX,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
    )
    
    // Resetear el offset cuando se inicia el borrado
    LaunchedEffect(isDeleting) {
        if (isDeleting) {
            offsetX = 0f
        }
    }
    
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        // Icono de borrado (aparece cuando se hace swipe)
        if (offsetX < -50f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                IconButton(
                    onClick = { onShowDeleteDialog(task) },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Color(0xFF388E3C),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete_task),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        
        // Contenido principal de la tarea
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = with(density) { animatedOffsetX.toDp() })
                .alpha(deleteAlpha)
                .pointerInput(task.id) {
                    detectDragGestures(
                        onDragEnd = {
                            if (offsetX < -200f) {
                                onShowDeleteDialog(task)
                            } else {
                                coroutineScope.launch {
                                    offsetX = 0f
                                }
                            }
                        }
                    ) { _, dragAmount ->
                        if (!isDeleting) {
                            offsetX = (offsetX + dragAmount.x).coerceAtLeast(-300f)
                        }
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTaskClick(task) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Radio button para marcar tarea como completada
                RadioButton(
                    selected = isCompleted, // Seleccionado si está completada
                    onClick = { 
                        // Lógica de clic según el estado de la tarea
                        when {
                            isLocked -> {
                                // Tarea bloqueada: no hacer nada (no se puede desmarcar)
                                // No hacer nada, el radio button permanece seleccionado
                            }
                            pendingSubtasksCount > 0 -> {
                                // Hay subtareas pendientes: mostrar toast
                                onShowToast(pendingSubtasksMessage)
                            }
                            else -> {
                                // Sin subtareas pendientes: permitir toggle normal
                                onTaskToggle(task)
                            }
                        }
                    },
                    enabled = !isLocked, // Deshabilitado si está bloqueada
                    colors = RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.primary,
                        unselectedColor = MaterialTheme.colorScheme.outline,
                        disabledSelectedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        disabledUnselectedColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
                    )
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Contenido de la tarea (título y fecha)
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Título de la tarea
                    Text(
                        text = task.titulo,
                        style = MaterialTheme.typography.bodyLarge,
                        color = when {
                            isFullyCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) // Gris para tareas completamente terminadas
                            isCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        textDecoration = if (isFullyCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    
                    // Fecha de creación
                    Text(
                        text = formatDate(task.fechaCreacion),
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            isFullyCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) // Más gris para tareas completamente terminadas
                            isCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        }
                    )
                    
                    // Fecha de vencimiento (si existe y la tarea no está completada)
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

                    // Categoría (si existe)
                    if (category != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        CategoryChip(category = category)
                    }
                }
                
                // Contador de subtareas pendientes e icono de información
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Contador de subtareas pendientes
                    if (pendingSubtasksCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(
                                    when {
                                        isFullyCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f) // Más gris para tareas completamente terminadas
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
                                    isFullyCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) // Más gris para tareas completamente terminadas
                                    isCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                fontWeight = MaterialTheme.typography.labelSmall.fontWeight
                            )
                        }
                    }
                    
                    // Icono de información para navegar a subtareas
                    IconButton(
                        onClick = { 
                            // Solo navegar si la tarea tiene subtareas (contador visible)
                            if (pendingSubtasksCount > 0) {
                                onNavigateToSubtasks(task) 
                            }
                        },
                        enabled = pendingSubtasksCount > 0, // Solo habilitado si tiene contador de subtareas
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(R.string.view_subtasks),
                            tint = when {
                                pendingSubtasksCount == 0 -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) // Deshabilitado si no tiene contador
                                isFullyCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                isCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                else -> MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            // Línea divisoria
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

/**
 * Función para formatear la fecha de manera legible
 */
private fun formatDate(date: java.util.Date): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return formatter.format(date)
}