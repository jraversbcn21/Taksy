package com.example.taksy.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.example.taksy.R
import com.example.taksy.data.Category
import com.example.taksy.data.DefaultCategories
import java.util.Locale

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
    
    /** Idiomas de la app; debe coincidir con res/xml/locales_config.xml. */
    private val SUPPORTED_LANGUAGES = listOf("es", "en")

    /** Nombre traducible de cada icono de categoría por defecto. */
    private val localizedNameRes: Map<String, Int> = mapOf(
        "work" to R.string.category_work,
        "person" to R.string.category_personal,
        "shopping_cart" to R.string.category_shopping,
        "favorite" to R.string.category_health,
        "health" to R.string.category_health,
        "school" to R.string.category_study,
        "home" to R.string.category_home,
        "fitness_center" to R.string.category_exercise,
        "more_horiz" to R.string.category_others
    )

    @Volatile
    private var knownDefaultNamesCache: Map<String, Set<String>>? = null

    /**
     * Nombres que una categoría por defecto puede tener guardados, por icono, en cualquier
     * idioma soportado (los del seed, los de category_* y los heredados de MIGRATION_4_5).
     * No depende del idioma actual, así que se calcula una sola vez.
     */
    private fun knownDefaultNames(context: Context): Map<String, Set<String>> =
        knownDefaultNamesCache ?: buildKnownDefaultNames(context).also { knownDefaultNamesCache = it }

    private fun buildKnownDefaultNames(context: Context): Map<String, Set<String>> {
        val names = mutableMapOf<String, MutableSet<String>>()
        SUPPORTED_LANGUAGES.forEach { language ->
            val config = Configuration(context.resources.configuration).apply {
                setLocale(Locale.forLanguageTag(language))
            }
            val resources = context.createConfigurationContext(config).resources
            resources.getStringArray(R.array.default_category_names)
                .zip(DefaultCategories.icons)
                .forEach { (name, icon) -> names.getOrPut(icon) { mutableSetOf() } += name }
            localizedNameRes.forEach { (icon, resId) ->
                names.getOrPut(icon) { mutableSetOf() } += resources.getString(resId)
            }
        }
        names.getOrPut("school") { mutableSetOf() } += context.getString(R.string.category_study_legacy)
        return names
    }

    /**
     * Decide qué nombre mostrar. Una categoría por defecto que el usuario no ha renombrado
     * se traduce al idioma actual; cualquier otra muestra su [nombre] tal cual.
     */
    fun resolveDisplayName(nombre: String, knownDefaultNames: Set<String>, localizedName: String?): String =
        if (localizedName != null && nombre.trim() in knownDefaultNames) localizedName else nombre

    /**
     * Obtiene el nombre de la categoría para mostrar en el idioma actual
     */
    @Composable
    fun getCategoryName(category: Category): String {
        val context = LocalContext.current
        val knownNames = remember(context) { knownDefaultNames(context) }
        val localizedName = localizedNameRes[category.icono]?.let { context.getString(it) }
        return resolveDisplayName(
            nombre = category.nombre,
            knownDefaultNames = knownNames[category.icono].orEmpty(),
            localizedName = localizedName
        )
    }
}

// Función de extensión para facilitar el uso
fun getCategoryIcon(iconName: String) = CategoryUtils.getCategoryIcon(iconName)
