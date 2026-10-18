package com.example.taksy.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.taksy.R

/**
 * Icono de la app; [size] es el diámetro del círculo visible. Sus marcas (check, líneas, TASKS) son transparentes en el PNG: el disco blanco
 * de debajo hace que se vean blancas también en modo oscuro (igual que en splash_icon.xml).
 */
@Composable
fun AppIcon(size: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape)) {
        // Un pelo más pequeño que el icono para que el blanco no asome por el borde
        Box(Modifier.fillMaxSize().padding(size * 0.02f).clip(CircleShape).background(Color.White))
        // El círculo ocupa el 76 % central del PNG (780 de 1024 px): se escala para llenar [size]
        Image(
            painter = painterResource(R.drawable.ticksy_icon),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = 1024f / 780f; scaleY = 1024f / 780f }
        )
    }
}
