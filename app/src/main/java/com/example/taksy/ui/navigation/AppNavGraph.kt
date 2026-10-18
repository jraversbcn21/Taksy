package com.example.taksy.ui.navigation

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.taksy.data.Category
import com.example.taksy.data.Task
import com.example.taksy.data.TaskEstado
import com.example.taksy.data.TaskRecurrencia
import com.example.taksy.data.preferences.PreferencesRepository
import com.example.taksy.ui.screens.AboutScreen
import com.example.taksy.ui.screens.BackupScreen
import com.example.taksy.ui.screens.CategoryListScreen
import com.example.taksy.ui.screens.CategoryManagementScreen
import com.example.taksy.ui.screens.DailyReminderScreen
import com.example.taksy.ui.screens.OnboardingScreen
import com.example.taksy.ui.screens.SettingsScreen
import com.example.taksy.ui.screens.StatsScreen
import com.example.taksy.ui.screens.TaskDetailScreen
import com.example.taksy.ui.screens.TasksByCategoryScreen
import com.example.taksy.utils.DateUtils
import com.example.taksy.viewmodel.BackupViewModel
import com.example.taksy.viewmodel.CategoryViewModel
import com.example.taksy.viewmodel.TaskInput
import com.example.taksy.viewmodel.TaskViewModel
import com.example.taksy.viewmodel.ThemeViewModel
import kotlinx.coroutines.flow.map

private fun NavHostController.popOrHome() {
    if (!popBackStack()) navigate("category_list") { launchSingleTop = true }
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    taskViewModel: TaskViewModel,
    categoryViewModel: CategoryViewModel,
    themeViewModel: ThemeViewModel,
    isDarkMode: Boolean,
    currentLanguage: String,
    context: Context,
    onMenuClick: () -> Unit,
    onShowLanguageChangeDialog: (String) -> Unit
) {
    val preferences = remember { PreferencesRepository(context.applicationContext) }
    val onboardingCompleted = remember { preferences.isOnboardingCompleted() }
    val startDestination = if (onboardingCompleted) "category_list" else "onboarding"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("onboarding") {
            OnboardingScreen(
                onFinish = {
                    preferences.setOnboardingCompleted(true)
                    navController.navigate("category_list") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                },
                isDarkMode = isDarkMode
            )
        }

        composable("category_list") {
            val categories by categoryViewModel.getAllCategories().collectAsState(initial = emptyList())
            val allTasks by taskViewModel.getAllTasks().collectAsState(initial = emptyList())

            val taskCountByCategory = remember(allTasks) {
                allTasks
                    .filter { it.estado != TaskEstado.COMPLETADA }
                    .mapNotNull { it.categoriaId }
                    .groupingBy { it }
                    .eachCount()
            }
            // Total por lista para la barra de progreso. Se excluyen las ocurrencias completadas
            // de tareas recurrentes, igual que hace la lista de cada categoría.
            val totalTaskCountByCategory = remember(allTasks) {
                allTasks
                    .filterNot { it.recurrencia != TaskRecurrencia.NINGUNA && it.estado == TaskEstado.COMPLETADA }
                    .mapNotNull { it.categoriaId }
                    .groupingBy { it }
                    .eachCount()
            }
            val todayTasks = remember(allTasks) {
                allTasks
                    .filter { task -> task.fechaVencimiento?.let { DateUtils.isToday(it) } == true }
                    .sortedWith(compareBy<Task> { it.estado == TaskEstado.COMPLETADA }.thenBy { it.fechaVencimiento })
            }

            var globalSearchQuery by remember { mutableStateOf("") }
            val searchResults by taskViewModel.searchAllTasks(globalSearchQuery)
                .collectAsState(initial = emptyList())

            CategoryListScreen(
                categories = categories,
                taskCountByCategory = taskCountByCategory,
                totalTaskCountByCategory = totalTaskCountByCategory,
                todayTasks = todayTasks,
                isDarkMode = isDarkMode,
                currentLanguage = currentLanguage,
                onSettingsClick = onMenuClick,
                onCategoryClick = { category ->
                    navController.navigate("tasks_by_category/${category.id}")
                },
                onReorderCategories = { reorderedCategories ->
                    categoryViewModel.reorderCategories(reorderedCategories)
                },
                showToast = {},
                searchResults = searchResults,
                onSearchQueryChanged = { globalSearchQuery = it },
                onTaskClick = { task -> navController.navigate("task_detail/${task.id}") },
                onToggleTask = { task -> taskViewModel.toggleTaskStatus(task) }
            )
        }

        composable("category_management") {
            val categories by categoryViewModel.getAllCategories().collectAsState(initial = emptyList())
            val allTasks by taskViewModel.getAllTasks().collectAsState(initial = emptyList())
            // Todas las tareas (no solo pendientes): son las que se borran junto con la categoría
            val taskCountByCategory = remember(allTasks) {
                allTasks.mapNotNull { it.categoriaId }.groupingBy { it }.eachCount()
            }

            CategoryManagementScreen(
                categories = categories,
                taskCountByCategory = taskCountByCategory,
                onBackClick = { navController.popOrHome() },
                onAddCategory = { nombre, color, icono -> categoryViewModel.addCategory(nombre, color, icono) },
                onUpdateCategory = { category -> categoryViewModel.updateCategory(category) },
                onDeleteCategory = { category -> categoryViewModel.deleteCategory(category) },
                showToast = { message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show() },
                isDarkMode = isDarkMode
            )
        }

        composable("daily_reminders") {
            DailyReminderScreen(
                onBackClick = { navController.popOrHome() },
                isDarkMode = isDarkMode
            )
        }

        composable("tasks_by_category/{categoryId}") { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId")?.toLongOrNull()
            if (categoryId != null) {
                var category by remember { mutableStateOf<Category?>(null) }
                val tasks by taskViewModel.getTasksByCategoryId(categoryId).collectAsState(initial = emptyList())

                LaunchedEffect(categoryId) {
                    category = categoryViewModel.getCategoryById(categoryId)
                }

                category?.let { resolvedCategory ->
                    TasksByCategoryScreen(
                        category = resolvedCategory,
                        tasks = tasks,
                        taskViewModel = taskViewModel,
                        isDarkMode = isDarkMode,
                        onBackClick = { navController.popBackStack() },
                        onTaskClick = { task -> navController.navigate("task_detail/${task.id}") },
                        onTaskToggle = { task -> taskViewModel.toggleTaskStatus(task) },
                        onAddTask = { title, dueDate, prioridad, recurrencia ->
                            taskViewModel.addTask(TaskInput(title, dueDate, categoryId, prioridad, recurrencia))
                        }
                    )
                }
            }
        }

        composable("settings") {
            val themeMode by themeViewModel.themeMode.collectAsState()
            SettingsScreen(
                themeMode = themeMode,
                currentLanguage = currentLanguage,
                onThemeModeChange = { mode -> themeViewModel.setThemeMode(mode) },
                onLanguageSelected = { language ->
                    if (language != currentLanguage) onShowLanguageChangeDialog(language)
                },
                onDailyRemindersClick = { navController.navigate("daily_reminders") },
                onBackClick = { navController.popOrHome() },
                isDarkMode = isDarkMode
            )
        }

        composable("stats") {
            StatsScreen(onBackClick = { navController.popOrHome() }, isDarkMode = isDarkMode)
        }

        composable("about") {
            AboutScreen(onBackClick = { navController.popOrHome() }, isDarkMode = isDarkMode)
        }

        composable("backup") {
            val backupViewModel: BackupViewModel = hiltViewModel()
            BackupScreen(
                backupViewModel = backupViewModel,
                onBackClick = { navController.popOrHome() },
                isDarkMode = isDarkMode
            )
        }

        composable("task_detail/{taskId}") { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId")?.toLongOrNull()
            if (taskId != null) {
                // Se observa la tarea en vivo: completar la última subtarea la completa sola
                val task by remember(taskId) {
                    taskViewModel.getAllTasks().map { tasks -> tasks.firstOrNull { it.id == taskId } }
                }.collectAsState(initial = null)
                val subtasks by taskViewModel.getSubtasksByTaskId(taskId).collectAsState(initial = emptyList())
                val reminders by taskViewModel.getRemindersByTaskId(taskId).collectAsState(initial = emptyList())
                var category by remember { mutableStateOf<Category?>(null) }

                LaunchedEffect(task?.categoriaId) {
                    category = task?.categoriaId?.let { categoryViewModel.getCategoryById(it) }
                }

                task?.let { resolvedTask ->
                    TaskDetailScreen(
                        task = resolvedTask,
                        category = category,
                        subtasks = subtasks,
                        activeReminder = reminders.firstOrNull { it.activo },
                        isDarkMode = isDarkMode,
                        onBackClick = { navController.popBackStack() },
                        onAddSubtask = { title -> taskViewModel.addSubtask(taskId, title) },
                        onToggleSubtask = { subtask -> taskViewModel.toggleSubtaskStatus(subtask) },
                        onUpdateTask = { updatedTask -> taskViewModel.updateTask(updatedTask) },
                        onSetReminder = { date -> taskViewModel.setQuickReminder(taskId, resolvedTask.titulo, date) },
                        onDeleteReminder = { taskViewModel.deleteReminderForTask(taskId) }
                    )
                }
            }
        }
    }
}
