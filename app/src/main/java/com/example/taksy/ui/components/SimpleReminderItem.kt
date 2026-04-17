package com.example.taksy.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.taksy.data.Reminder
import com.example.taksy.data.TipoRecordatorio
import java.text.SimpleDateFormat
import java.util.*

/**
 * Componente simplificado para mostrar un recordatorio
 * Cards más pequeñas y simples
 */
@Composable
fun SimpleReminderItem(
    reminder: Reminder,
    onToggleStatus: (Long, Boolean) -> Unit,
    onDelete: (Reminder) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val frequencyText = when (reminder.tipoRecordatorio) {
        TipoRecordatorio.UNA_VEZ -> "Una vez"
        TipoRecordatorio.DIARIO -> "Diario"
        TipoRecordatorio.SEMANAL -> "Semanal"
        TipoRecordatorio.MENSUAL -> "Mensual"
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (reminder.activo) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = if (reminder.activo) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Contenido
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = reminder.titulo,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = if (reminder.activo) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (!reminder.descripcion.isNullOrBlank()) {
                    Text(
                        text = reminder.descripcion,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (reminder.activo) 
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateFormat.format(reminder.fechaRecordatorio),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (reminder.activo) 
                            MaterialTheme.colorScheme.primary
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "• $frequencyText",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (reminder.activo) 
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            
            // Botón de eliminar
            IconButton(
                onClick = { onDelete(reminder) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar recordatorio",
                    tint = if (reminder.activo) 
                        MaterialTheme.colorScheme.error
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
