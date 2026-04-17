package com.example.taksy.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.taksy.data.Subtask
import com.example.taksy.data.TaskEstado

/**
 * Componente que representa una subtarea individual
 */
@Composable
fun SubtaskItem(
    subtask: Subtask,
    onToggle: (Subtask) -> Unit,
    onDelete: (Subtask) -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompleted = subtask.estado == TaskEstado.COMPLETADA
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox para marcar como completada
        Surface(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape),
            color = if (isCompleted) MaterialTheme.colorScheme.primary else Color.Transparent,
            border = androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                color = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completada",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Título de la subtarea
        Text(
            text = subtask.titulo,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isCompleted) {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.weight(1f)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Botón de eliminar
        IconButton(
            onClick = { onDelete(subtask) },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Eliminar subtarea",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
