package com.example.taksy.domain

/**
 * Etapas de la planta de Estadísticas según el total de tareas completadas. Solo crece: no hay
 * penalización por días sin completar nada.
 */
enum class PlantStage(val minCompleted: Int) {
    SEED(0),
    SPROUT(5),
    SEEDLING(20),
    PLANT(60),
    TREE(150);

    val next: PlantStage? get() = entries.getOrNull(ordinal + 1)

    /** Avance hacia la siguiente etapa (0..1); 1 si ya es la última. */
    fun progress(completed: Int): Float {
        val target = next ?: return 1f
        return ((completed - minCompleted).toFloat() / (target.minCompleted - minCompleted)).coerceIn(0f, 1f)
    }

    /** Tareas que faltan para la siguiente etapa; 0 si ya es la última. */
    fun remaining(completed: Int): Int =
        next?.let { (it.minCompleted - completed).coerceAtLeast(0) } ?: 0

    companion object {
        fun of(completed: Int): PlantStage = entries.last { completed >= it.minCompleted }
    }
}
