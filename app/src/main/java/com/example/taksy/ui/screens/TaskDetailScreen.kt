package com.example.taksy.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.example.taksy.R
import com.example.taksy.data.Subtask
import com.example.taksy.data.Task
import com.example.taksy.data.TaskEstado
import com.example.taksy.data.TaskRecurrencia
import com.example.taksy.ui.components.Toast
import kotlinx.coroutines.delay
import com.example.taksy.ui.components.SubtaskList
import com.example.taksy.ui.components.ReminderItem
import com.example.taksy.ui.components.AddReminderDialog
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
    var descriptionText by remember(task.id) { mutableStateOf(task.descripcion ?: "") }

    LaunchedEffect(descriptionText) {
        delay(500)
        val newDesc = descriptionText.ifBlank { null }
        if (newDesc != task.descripcion) {
            onUpdateTask(task.copy(descripcion = newDesc))
        }
    }
    
    // Obtener el mensaje de toast multilingüe
    val allSubtasksCompletedMessage = stringResource(R.string.all_subtasks_completed)
    
    val showToastMessage = { message: String ->
        toastMessage = message
        showToast = true
    }
    
    // Enfocar automáticamente el campo cuando se muestra
    LaunchedEffect(showInlineInput) {
        if (showInlineInput) {
            focusRequester.requestFocus()
        }
    }
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top App Bar
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
            
            // Contenido principal
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // Sección de Notas/Descripción
                item(key = "description_section") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.task_description_label),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        OutlinedTextField(
                            value = descriptionText,
                            onValueChange = { descriptionText = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.task_description_placeholder),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            },
                            minLines = 2,
                            maxLines = 6,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = MaterialTheme.typography.bodyLarge.fontSize
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                cursorColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    }
                }

                // Sección de Recurrencia
                item(key = "recurrence_section") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color(0xFF7E57C2),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.recurrence),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        val recurrences = listOf(
                            TaskRecurrencia.NINGUNA to R.string.recurrence_none,
                            TaskRecurrencia.DIARIA to R.string.recurrence_daily,
                            TaskRecurrencia.SEMANAL to R.string.recurrence_weekly,
                            TaskRecurrencia.MENSUAL to R.string.recurrence_monthly,
                            TaskRecurrencia.ANUAL to R.string.recurrence_yearly
                        )
                        val recurrenceColor = Color(0xFF7E57C2)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            recurrences.forEach { (recurrence, labelRes) ->
                                val isSelected = task.recurrencia == recurrence
                                val chipColor = if (recurrence == TaskRecurrencia.NINGUNA) MaterialTheme.colorScheme.outline else recurrenceColor
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (task.recurrencia != recurrence) {
                                            onUpdateTask(task.copy(recurrencia = recurrence))
                                        }
                                    },
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
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    }
                }

                // Sección de Subtareas
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
                        IconButton(
                            onClick = { 
                                if (task.estado != TaskEstado.COMPLETADA) {
                                    if (showInlineInput && newSubtaskTitle.isNotBlank()) {
                                        // Si hay texto en el input, añadir la subtarea
                                        onAddSubtask(newSubtaskTitle.trim())
                                        newSubtaskTitle = "" // Limpiar el campo pero mantener el input visible
                                    } else {
                                        // Si no hay input visible o está vacío, mostrar el input
                                        showInlineInput = true 
                                    }
                                }
                            }
                        ) {
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
                
                // Input inline para nueva subtarea
                if (showInlineInput) {
                    item(key = "inline_subtask_input") {
                        InlineSubtaskInput(
                            subtaskTitle = newSubtaskTitle,
                            onSubtaskTitleChange = { newSubtaskTitle = it },
                            onAddSubtask = { title ->
                                if (title.isNotBlank()) {
                                    onAddSubtask(title.trim())
                                    newSubtaskTitle = "" // Limpiar el campo pero mantener el input visible
                                    // No cambiar showInlineInput = false para mantener el input activo
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
                    // Lista de subtareas ordenadas (pendientes primero, completadas al final)
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
                                // Solo permitir toggle si la tarea no está completada
                                if (task.estado != TaskEstado.COMPLETADA) {
                                    // Verificar si estaba pendiente antes de cambiar
                                    val wasPending = subtask.estado == TaskEstado.PENDIENTE
                                    
                                    // Contar subtareas pendientes ANTES de hacer el cambio
                                    val pendingSubtasksBefore = sortedSubtasks.count { it.estado == TaskEstado.PENDIENTE }

                                    onToggleSubtask(subtask)

                                    if (wasPending && pendingSubtasksBefore == 1) {
                                        showToastMessage(allSubtasksCompletedMessage)
                                    }
                                }
                            }
                        )
                    }
                }
                
            }
            
            // Botón flotante para añadir subtarea
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                FloatingActionButton(
                    onClick = { 
                        if (task.estado != TaskEstado.COMPLETADA) {
                            if (showInlineInput && newSubtaskTitle.isNotBlank()) {
                                // Si hay texto en el input, añadir la subtarea
                                onAddSubtask(newSubtaskTitle.trim())
                                newSubtaskTitle = "" // Limpiar el campo pero mantener el input visible
                            } else {
                                // Si no hay input visible o está vacío, mostrar el input
                                showInlineInput = true 
                            }
                        }
                    },
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
        
        // Toast para mostrar mensajes - FUERA del Column
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

@Composable
private fun SubtaskItem(
    subtask: Subtask,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompleted = subtask.estado == TaskEstado.COMPLETADA
    
    val alpha by animateFloatAsState(
        targetValue = if (isCompleted) 0.6f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isCompleted) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
    )
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .scale(scale)
            .alpha(alpha),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Radio button
        RadioButton(
            selected = isCompleted,
            onClick = onToggle,
            enabled = !isCompleted, // Deshabilitado si está completada
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.outline,
                disabledSelectedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                disabledUnselectedColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
            )
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Texto de la subtarea
        Text(
            text = subtask.titulo,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isCompleted) {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
        )
    }
    
    // Línea divisoria
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun InlineSubtaskInput(
    subtaskTitle: String,
    onSubtaskTitleChange: (String) -> Unit,
    onAddSubtask: (String) -> Unit,
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
        
        // Campo de texto
        BasicTextField(
            value = subtaskTitle,
            onValueChange = onSubtaskTitleChange,
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
                    if (subtaskTitle.isNotBlank()) {
                        onAddSubtask(subtaskTitle.trim())
                    }
                }
            ),
            decorationBox = { innerTextField ->
                if (subtaskTitle.isEmpty()) {
                    Text(
                        text = stringResource(R.string.add_subtask),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                innerTextField()
            }
        )
        
        // Botón de cancelar
        if (subtaskTitle.isNotBlank()) {
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}

@Composable
private fun AddSubtaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.new_subtask))
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.add_subtask_hint),
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
                            text = stringResource(R.string.add_subtask_hint),
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
                        onConfirm(title.trim())
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text(stringResource(R.string.add_subtask))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
