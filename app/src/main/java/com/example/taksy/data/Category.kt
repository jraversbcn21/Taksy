package com.example.taksy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que representa una categoría/etiqueta en la base de datos
 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val color: String, // Color en formato hexadecimal (ej: "#FF5722")
    val icono: String = "label", // Nombre del icono (por defecto "label")
    val orden: Int = 0 // Orden de visualización
)

/**
 * Categorías predefinidas del sistema
 */
object DefaultCategories {
    val WORK = Category(
        id = 1,
        nombre = "Trabajo",
        color = "#2196F3", // Azul
        icono = "work",
        orden = 1
    )
    
    val PERSONAL = Category(
        id = 2,
        nombre = "Personal",
        color = "#4CAF50", // Verde
        icono = "person",
        orden = 2
    )
    
    val SHOPPING = Category(
        id = 3,
        nombre = "Compras",
        color = "#FF9800", // Naranja
        icono = "shopping_cart",
        orden = 3
    )
    
    val HEALTH = Category(
        id = 4,
        nombre = "Salud",
        color = "#E91E63", // Rosa
        icono = "favorite",
        orden = 4
    )
    
    val HOME = Category(
        id = 5,
        nombre = "Hogar",
        color = "#9C27B0", // Púrpura
        icono = "home",
        orden = 5
    )
    
    val STUDY = Category(
        id = 6,
        nombre = "Estudios",
        color = "#607D8B", // Azul gris
        icono = "school",
        orden = 6
    )
    
    val EXERCISE = Category(
        id = 7,
        nombre = "Ejercicio",
        color = "#00BCD4", // Cian
        icono = "fitness_center",
        orden = 7
    )
    
    val OTHERS = Category(
        id = 8,
        nombre = "Otros",
        color = "#795548", // Marrón
        icono = "more_horiz",
        orden = 8
    )
    
    fun getAll(): List<Category> = listOf(WORK, PERSONAL, SHOPPING, HEALTH, HOME, STUDY, EXERCISE, OTHERS)
}
