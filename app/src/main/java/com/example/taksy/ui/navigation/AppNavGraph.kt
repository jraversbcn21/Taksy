package com.example.taksy.ui.navigation

import android.content.Context
import androidx.activity.ComponentActivity
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
import com.example.taksy.data.preferences.PreferencesRepository
import com.example.taksy.ui.screens.AboutScreen
import com.example.taksy.ui.screens.BackupScreen
import com.example.taksy.ui.screens.CategoryListScreen
import com.example.taksy.ui.screens.DailyReminderScreen
import com.example.taksy.ui.screens.LanguageSettingsScreen
import com.example.taksy.ui.screens.OnboardingScreen
import com.example.taksy.ui.screens.RemindersScreen
import com.example.taksy.ui.screens.StatsScreen
import com.example.taksy.ui.screens.TaskDetailScreen
import com.example.taksy.ui.screens.TasksByCategoryScreen
import com.example.taksy.ui.screens.ThemeSettingsScreen
import com.example.taksy.viewmodel.BackupViewModel
import com.example.taksy.viewmodel.CategoryViewModel
import com.example.taksy.viewmodel.TaskInput
import com.example.taksy.viewmodel.TaskViewModel
import com.example.taksy.viewmodel.ThemeViewModel

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
    activity: ComponentActivity,
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
                }
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

            var globalSearchQuery by remember { mutableStateOf("") }
            val searchResults by taskViewModel.searchAllTasks(globalSearchQuery)
                .collectAsState(initial = emptyList())

            CategoryListScreen(
                categories = categories,
                taskCountByCategory = taskCountByCategory,
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
                onTaskClick = { task -> navController.navigate("task_detail/${task.id}") }
            )
        }

        composable("reminders") {
            val reminders by taskViewModel.getAllActiveReminders().collectAsState(initial = emptyList())

            RemindersScreen(
                reminders = reminders,
                onBackClick = { navController.popBackStack() },
                onAddReminder = { titulo, descripcion, fecha, tipo ->
                    taskViewModel.addReminder(0, titulo, descripcion, fecha, tipo)
                },
                onToggleReminder = { reminderId, activo ->
                    taskViewModel.updateReminderStatus(reminderId, activo)
                },
                onDeleteReminder = { reminder -> taskViewModel.deleteReminder(reminder) }
            )
        }

        composable("daily_reminders") {
            DailyReminderScreen(
                onBackClick = { navController.popOrHome() },
                isDarkMode = isDarkMode,
                activity = activity
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
                        onBackClick = { navController.popBackStack() },
                        onTaskClick = { task -> navController.navigate("task_detail/${task.id}") },
                        onTaskToggle = { task -> taskViewModel.toggleTaskStatus(task) },
                        onTaskDelete = { task -> taskViewModel.deleteTask(task) },
                        onAddTask = { title, dueDate, prioridad, recurrencia ->
                            taskViewModel.addTask(TaskInput(title, dueDate, categoryId, prioridad, recurrencia))
                        },
                        showToast = {}
                    )
                }
            }
        }

        composable("theme_settings") {
            ThemeSettingsScreen(
                isDarkMode = isDarkMode,
                onThemeChange = { isDark -> themeViewModel.setDarkMode(isDark) },
                onBackClick = { navController.popOrHome() }
            )
        }

        composable("language_settings") {
            LanguageSettingsScreen(
                currentLanguage = currentLanguage,
                onLanguageChange = { language -> themeViewModel.setLanguage(language) },
                onShowLanguageChangeDialog = onShowLanguageChangeDialog,
                onBackClick = { navController.popOrHome() }
            )
        }

        composable("stats") {
            StatsScreen(onBackClick = { navController.popOrHome() })
        }

        composable("about") {
            AboutScreen(onBackClick = { navController.popOrHome() })
        }

        composable("backup") {
            val backupViewModel: BackupViewModel = hiltViewModel()
            BackupScreen(
                backupViewModel = backupViewModel,
                onBackClick = { navController.popOrHome() }
            )
        }

        composable("task_detail/{taskId}") { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId")?.toLongOrNull()
            if (taskId != null) {
                var task by remember { mutableStateOf<Task?>(null) }
                val subtasks by taskViewModel.getSubtasksByTaskId(taskId).collectAsState(initial = emptyList())

                LaunchedEffect(taskId) {
                    task = taskViewModel.getTaskById(taskId)
                }

                task?.let { resolvedTask ->
                    TaskDetailScreen(
                        task = resolvedTask,
                        subtasks = subtasks,
                        onBackClick = { navController.popBackStack() },
                        onAddSubtask = { title -> taskViewModel.addSubtask(taskId, title) },
                        onToggleSubtask = { subtask -> taskViewModel.toggleSubtaskStatus(subtask) },
                        onUpdateTask = { updatedTask ->
                            task = updatedTask
                            taskViewModel.updateTask(updatedTask)
                        }
                    )
                }
            }
        }
    }
}
