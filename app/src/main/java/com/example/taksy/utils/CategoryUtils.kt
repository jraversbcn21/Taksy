package com.example.taksy.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.example.taksy.R

/**
 * Utilidades para manejar iconos de categorías
 */
object CategoryUtils {
    
    /**
     * Iconos que el usuario puede elegir al crear/editar una categoría, en orden de presentación.
     * Fuente única: el diálogo y el renderizado usan este mismo mapa.
     */
    val selectableIcons: List<Pair<String, ImageVector>> = listOf(
        "work" to Icons.Default.Build,
        "person" to Icons.Default.Person,
        "shopping_cart" to Icons.Default.ShoppingCart,
        "favorite" to Icons.Default.Favorite,
        "school" to Icons.Default.Create,
        "home" to Icons.Default.Home,
        "star" to Icons.Default.Star,
        "schedule" to Icons.Default.DateRange,
        "business" to Icons.Default.Info,
        "sports" to Icons.Default.Notifications
    )

    /**
     * Nombres que solo existen en datos ya guardados: categorías por defecto
     * (fitness_center, more_horiz) o heredadas de MIGRATION_4_5 (health, label).
     * "health" es un alias intencionado de "favorite" (misma categoría Salud).
     */
    private val legacyIcons: Map<String, ImageVector> = mapOf(
        "fitness_center" to Icons.Default.Star,
        "more_horiz" to Icons.Default.MoreVert,
        "health" to Icons.Default.Favorite,
        "label" to Icons.Default.Info
    )

    private val iconsByName: Map<String, ImageVector> = selectableIcons.toMap() + legacyIcons

    /**
     * Obtiene el icono correspondiente al nombre del icono
     */
    fun getCategoryIcon(iconName: String): ImageVector =
        iconsByName[iconName] ?: Icons.Default.Info // Icono por defecto
    
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
