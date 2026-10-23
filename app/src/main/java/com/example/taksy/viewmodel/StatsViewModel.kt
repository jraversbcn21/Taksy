package com.example.taksy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taksy.data.Category
import com.example.taksy.data.Task
import com.example.taksy.data.TaskEstado
import com.example.taksy.data.preferences.PreferencesRepository
import com.example.taksy.domain.StreakCalculator
import com.example.taksy.repository.CategoryRepository
import com.example.taksy.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

/** Tareas completadas en un día; la etiqueta (inicial del día) se localiza en la UI. */
data class DayCompletion(val day: Date, val count: Int)

data class CategoryStats(
    val category: Category,
    val total: Int,
    val completed: Int
)

data class StatsUiState(
    val totalTasks: Int = 0,
    val pendingTasks: Int = 0,
    val completedTasks: Int = 0,
    val completionRate: Int = 0,
    val activeCategoriesCount: Int = 0,
    val streakDays: Int = 0,
    /** Completadas acumuladas para la planta; no baja al borrar tareas (ver PreferencesRepository). */
    val plantCompleted: Int = 0,
    val last7Days: List<DayCompletion> = emptyList(),
    val topCategories: List<CategoryStats> = emptyList()
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    taskRepository: TaskRepository,
    categoryRepository: CategoryRepository,
    private val preferences: PreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = combine(
        taskRepository.getAllTasks(),
        categoryRepository.getAllCategories()
    ) { tasks, categories ->
        computeStats(tasks, categories)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatsUiState()
    )

    private fun computeStats(tasks: List<Task>, categories: List<Category>): StatsUiState {
        val total = tasks.size
        val completed = tasks.count { it.estado == TaskEstado.COMPLETADA }
        val pending = total - completed
        val rate = if (total == 0) 0 else (completed * 100 / total)

        val activeCategoriesCount = tasks
            .filter { it.estado == TaskEstado.PENDIENTE && it.categoriaId != null }
            .map { it.categoriaId }
            .toSet()
            .size

        val last7Days = buildLast7Days(tasks)
        val streak = StreakCalculator.compute(tasks.mapNotNull { it.fechaCompletada })

        val topCategories = categories
            .map { cat ->
                val catTasks = tasks.filter { it.categoriaId == cat.id }
                CategoryStats(
                    category = cat,
                    total = catTasks.size,
                    completed = catTasks.count { it.estado == TaskEstado.COMPLETADA }
                )
            }
            .filter { it.total > 0 }
            .sortedByDescending { it.total }
            .take(5)

        return StatsUiState(
            totalTasks = total,
            pendingTasks = pending,
            completedTasks = completed,
            completionRate = rate,
            activeCategoriesCount = activeCategoriesCount,
            streakDays = streak,
            plantCompleted = preferences.raisePlantCompleted(completed),
            last7Days = last7Days,
            topCategories = topCategories
        )
    }

    private fun buildLast7Days(tasks: List<Task>): List<DayCompletion> {
        val result = mutableListOf<DayCompletion>()
        val cal = Calendar.getInstance()
        startOfDay(cal)
        val today = cal.timeInMillis
        for (i in 6 downTo 0) {
            val dayStart = today - i * 86_400_000L
            val dayEnd = dayStart + 86_400_000L
            val count = tasks.count {
                val d = it.fechaCompletada?.time ?: return@count false
                d in dayStart until dayEnd
            }
            result.add(DayCompletion(Date(dayStart), count))
        }
        return result
    }


    private fun startOfDay(cal: Calendar) {
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
    }
}
