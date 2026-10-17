package com.example.taksy.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CategoryUtilsTest {

    @Test
    fun `every selectable icon renders as the icon shown in the picker`() {
        CategoryUtils.selectableIcons.forEach { (name, icon) ->
            assertEquals("icono '$name'", icon, CategoryUtils.getCategoryIcon(name))
        }
    }

    @Test
    fun `school maps to a study icon, not Settings`() {
        assertEquals(Icons.Default.Create, CategoryUtils.getCategoryIcon("school"))
        assertNotEquals(Icons.Default.Settings, CategoryUtils.getCategoryIcon("school"))
    }

    @Test
    fun `legacy health is an alias of favorite`() {
        assertEquals(Icons.Default.Favorite, CategoryUtils.getCategoryIcon("health"))
        assertEquals(CategoryUtils.getCategoryIcon("favorite"), CategoryUtils.getCategoryIcon("health"))
    }

    // ── resolveDisplayName ────────────────────────────────────────────────

    private val workDefaults = setOf("Trabajo", "Work")

    @Test
    fun `unrenamed default category is shown in the current language`() {
        assertEquals("Work", CategoryUtils.resolveDisplayName("Trabajo", workDefaults, "Work"))
        assertEquals("Trabajo", CategoryUtils.resolveDisplayName("Work", workDefaults, "Trabajo"))
    }

    @Test
    fun `renamed default category keeps the user's name`() {
        assertEquals("Oficina", CategoryUtils.resolveDisplayName("Oficina", workDefaults, "Work"))
    }

    @Test
    fun `user category with an icon that has no default name shows its own name`() {
        assertEquals("Viajes", CategoryUtils.resolveDisplayName("Viajes", emptySet(), null))
    }

    @Test
    fun `user category named like a default but with another icon is not translated`() {
        // "Trabajo" con icono star: star no tiene nombre traducible, así que localizedName es null
        assertEquals("Trabajo", CategoryUtils.resolveDisplayName("Trabajo", emptySet(), null))
    }

    @Test
    fun `surrounding whitespace does not prevent recognizing a default name`() {
        assertEquals("Work", CategoryUtils.resolveDisplayName(" Trabajo ", workDefaults, "Work"))
    }

    @Test
    fun `unknown icon name falls back to Info`() {
        assertEquals(Icons.Default.Info, CategoryUtils.getCategoryIcon("does_not_exist"))
    }
}
