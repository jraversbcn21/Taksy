package com.example.taksy.data

import android.content.Context
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.taksy.R

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val color: String,
    val icono: String = "label",
    val orden: Int = 0
)

object DefaultCategories {

    private data class Metadata(val color: String, val icono: String)

    private val metadata: List<Metadata> = listOf(
        Metadata(color = "#2196F3", icono = "work"),
        Metadata(color = "#4CAF50", icono = "person"),
        Metadata(color = "#FF9800", icono = "shopping_cart"),
        Metadata(color = "#E91E63", icono = "favorite"),
        Metadata(color = "#9C27B0", icono = "home"),
        Metadata(color = "#607D8B", icono = "school"),
        Metadata(color = "#00BCD4", icono = "fitness_center"),
        Metadata(color = "#795548", icono = "more_horiz")
    )

    fun getAll(context: Context): List<Category> {
        val names = context.resources.getStringArray(R.array.default_category_names)
        check(names.size == metadata.size) {
            "default_category_names must have ${metadata.size} entries, got ${names.size}"
        }
        return metadata.mapIndexed { index, meta ->
            Category(
                id = (index + 1).toLong(),
                nombre = names[index],
                color = meta.color,
                icono = meta.icono,
                orden = index + 1
            )
        }
    }
}
