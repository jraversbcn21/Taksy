package com.example.taksy.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.taksy.R
import com.example.taksy.data.Category
import com.example.taksy.utils.getCategoryIcon
import com.example.taksy.utils.CategoryUtils

/**
 * Componente para mostrar una categoría en la lista
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CategoryItem(
    category: Category,
    taskCount: Int = 0,
    onClick: () -> Unit = {},
    isDragging: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.05f else 1f,
        animationSpec = tween(200),
        label = "scale"
    )
    val elevation by animateFloatAsState(
        targetValue = if (isDragging) 8f else 2f,
        animationSpec = tween(200),
        label = "elevation"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .scale(scale)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = elevation.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono de la categoría con color de fondo
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(android.graphics.Color.parseColor(category.color)).copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            getCategoryIcon(category.icono),
                            contentDescription = null,
                            tint = Color(android.graphics.Color.parseColor(category.color)),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Información de la categoría
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = CategoryUtils.getCategoryName(category.icono),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Mostrar conteo de tareas
                Text(
                    text = if (taskCount == 1) 
                        stringResource(R.string.task_count_singular, taskCount) 
                    else 
                        stringResource(R.string.task_count_plural, taskCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Badge con el número de tareas
            if (taskCount > 0) {
                Badge(
                    containerColor = Color(android.graphics.Color.parseColor(category.color)).copy(alpha = 0.2f),
                    contentColor = Color(android.graphics.Color.parseColor(category.color)),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = taskCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
