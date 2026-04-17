package com.example.taksy.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Utilidades para adaptar la UI a diferentes dispositivos
 */
object DeviceUtils {
    
    /**
     * Calcula el padding inferior para FAB basado en las características del dispositivo
     * Considera la densidad de pantalla, altura de la pantalla y tipo de dispositivo
     */
    @Composable
    fun getFabBottomPadding(): Dp {
        val configuration = LocalConfiguration.current
        val density = LocalDensity.current
        
        val screenHeight = configuration.screenHeightDp
        val screenWidth = configuration.screenWidthDp
        val densityDpi = density.density
        
        // Calcular padding base basado en la densidad (reducido para mejor posicionamiento)
        val basePadding = when {
            densityDpi >= 3.5f -> 20.dp  // Dispositivos de alta densidad (xxhdpi+)
            densityDpi >= 2.5f -> 16.dp  // Dispositivos de densidad alta (xhdpi)
            densityDpi >= 1.5f -> 12.dp  // Dispositivos de densidad media (hdpi)
            else -> 8.dp                 // Dispositivos de densidad baja (mdpi)
        }
        
        // Ajustar basado en el tamaño de pantalla
        val sizeAdjustment = when {
            screenHeight >= 800 -> 6.dp   // Pantallas grandes (tablets)
            screenHeight >= 600 -> 2.dp   // Pantallas medianas
            else -> 0.dp                  // Pantallas pequeñas
        }
        
        // Ajustar basado en la relación de aspecto
        val aspectRatio = screenHeight.toFloat() / screenWidth.toFloat()
        val aspectAdjustment = when {
            aspectRatio >= 2.0f -> 2.dp   // Pantallas muy altas (18:9+)
            aspectRatio >= 1.8f -> 1.dp   // Pantallas altas (16:9)
            else -> 0.dp                  // Pantallas cuadradas o anchas
        }
        
        return basePadding + sizeAdjustment + aspectAdjustment
    }
    
    /**
     * Calcula el padding inferior específico para FAB de subtareas
     * Más ajustado que el FAB principal para mejor posicionamiento
     */
    @Composable
    fun getSubtaskFabBottomPadding(): Dp {
        val configuration = LocalConfiguration.current
        val density = LocalDensity.current
        
        val screenHeight = configuration.screenHeightDp
        val densityDpi = density.density
        
        // Padding más grande para subtareas (más arriba)
        val basePadding = when {
            densityDpi >= 3.5f -> 56.dp  // Dispositivos de alta densidad
            densityDpi >= 2.5f -> 52.dp  // Dispositivos de densidad alta
            densityDpi >= 1.5f -> 48.dp  // Dispositivos de densidad media
            else -> 44.dp                // Dispositivos de densidad baja
        }
        
        // Ajuste basado en el tamaño de pantalla
        val sizeAdjustment = when {
            screenHeight >= 800 -> 8.dp   // Pantallas grandes
            screenHeight >= 600 -> 4.dp   // Pantallas medianas
            else -> 0.dp                  // Pantallas pequeñas
        }
        
        return basePadding + sizeAdjustment
    }
    
    /**
     * Calcula el padding lateral para FAB
     */
    @Composable
    fun getFabSidePadding(): Dp {
        val configuration = LocalConfiguration.current
        val density = LocalDensity.current
        
        val densityDpi = density.density
        
        return when {
            densityDpi >= 3.5f -> 20.dp  // Dispositivos de alta densidad
            densityDpi >= 2.5f -> 18.dp  // Dispositivos de densidad alta
            densityDpi >= 1.5f -> 16.dp  // Dispositivos de densidad media
            else -> 14.dp                // Dispositivos de densidad baja
        }
    }
}
