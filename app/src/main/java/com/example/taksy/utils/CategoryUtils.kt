package com.example.taksy.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.taksy.R

/**
 * Utilidades para manejar iconos de categorías
 */
object CategoryUtils {
    
    /**
     * Obtiene el icono correspondiente al nombre del icono
     */
    fun getCategoryIcon(iconName: String) = when (iconName) {
        "work" -> Icons.Default.Build
        "person" -> Icons.Default.Person
        "shopping_cart" -> Icons.Default.ShoppingCart
        "favorite" -> Icons.Default.Favorite
        "school" -> Icons.Default.Settings
        "home" -> Icons.Default.Home
        "fitness_center" -> Icons.Default.Star
        "more_horiz" -> Icons.Default.MoreVert
        "health" -> Icons.Default.Favorite
        "label" -> Icons.Default.Info
        else -> Icons.Default.Info // Icono por defecto
    }
    
    /**
     * Obtiene el nombre de la categoría en el idioma actual
     */
    @Composable
    fun getCategoryName(iconName: String): String {
        val context = LocalContext.current
        return when (iconName) {
            "work" -> context.getString(R.string.category_work)
            "person" -> context.getString(R.string.category_personal)
            "shopping_cart" -> context.getString(R.string.category_shopping)
            "favorite" -> context.getString(R.string.category_health)
            "school" -> context.getString(R.string.category_study)
            "home" -> context.getString(R.string.category_home)
            "fitness_center" -> context.getString(R.string.category_exercise)
            "more_horiz" -> context.getString(R.string.category_others)
            "health" -> context.getString(R.string.category_health)
            "label" -> context.getString(R.string.category_personal)
            else -> context.getString(R.string.category_personal) // Por defecto
        }
    }
}

// Función de extensión para facilitar el uso
fun getCategoryIcon(iconName: String) = CategoryUtils.getCategoryIcon(iconName)
