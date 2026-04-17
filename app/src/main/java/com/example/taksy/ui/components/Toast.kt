package com.example.taksy.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay

@Composable
fun Toast(
    message: String,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    backgroundColor: Color = Color(0xFF4CAF50).copy(alpha = 0.9f),
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            animationSpec = tween(300),
            initialOffsetY = { it }
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            animationSpec = tween(200),
            targetOffsetY = { it }
        ) + fadeOut(animationSpec = tween(200)),
        modifier = modifier.zIndex(9999f) // Z-index más alto para estar por encima de la navbar
    ) {
        Card(
            modifier = Modifier
                .padding(horizontal = 32.dp, vertical = 12.dp)
                .padding(bottom = 50.dp), // Padding reducido para bajar un poco el toast
            shape = RoundedCornerShape(24.dp), // Forma de cápsula
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
    
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(2000)
            onDismiss()
        }
    }
}
