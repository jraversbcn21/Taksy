package com.example.taksy

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationManagerCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.taksy.ui.screens.CategoryListScreen
import com.example.taksy.ui.screens.DailyReminderScreen
import com.example.taksy.ui.screens.LanguageSettingsScreen
import com.example.taksy.ui.screens.OnboardingScreen
import com.example.taksy.ui.screens.RemindersScreen
import com.example.taksy.ui.screens.TaskDetailScreen
import com.example.taksy.ui.screens.TasksByCategoryScreen
import com.example.taksy.ui.screens.AboutScreen
import com.example.taksy.ui.screens.BackupScreen
import com.example.taksy.viewmodel.BackupViewModel
import com.example.taksy.ui.screens.ThemeSettingsScreen
import com.example.taksy.ui.theme.TicksyTheme
import com.example.taksy.utils.LocaleHelper
import com.example.taksy.viewmodel.CategoryViewModel
import com.example.taksy.viewmodel.TaskViewModel
import com.example.taksy.viewmodel.ThemeViewModel

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val themeViewModel: ThemeViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* permission result handled silently */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Smooth fade-in when activity is recreated (e.g., language change)
        if (savedInstanceState != null) {
            window.decorView.alpha = 0f
            window.decorView.animate().alpha(1f).setDuration(350).start()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Aplicar el idioma guardado antes de renderizar la UI
        LocaleHelper.applyLocale(this, LocaleHelper.getCurrentLanguage(this))

        setContent {
            MainScreen(activity = this)
        }
    }
}

@Composable
fun MainScreen(activity: ComponentActivity) {
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val taskViewModel: TaskViewModel = hiltViewModel()
    val categoryViewModel: CategoryViewModel = hiltViewModel()
    val navController = rememberNavController()

    val isDarkMode by themeViewModel.isDarkMode.collectAsStateWithLifecycle()
    val currentLanguage by themeViewModel.currentLanguage.collectAsStateWithLifecycle()

    TicksyTheme(darkTheme = isDarkMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            DrawerScreen(
                navController = navController,
                taskViewModel = taskViewModel,
                categoryViewModel = categoryViewModel,
                isDarkMode = isDarkMode,
                currentLanguage = currentLanguage,
                themeViewModel = themeViewModel,
                context = LocalContext.current,
                activity = activity
            )
        }
    }
}

private const val ONBOARDING_PREFS = "onboarding_prefs"
private const val ONBOARDING_COMPLETED_KEY = "onboarding_completed"

@Composable
fun MainContent(
    navController: androidx.navigation.NavHostController,
    taskViewModel: TaskViewModel,
    categoryViewModel: CategoryViewModel,
    isDarkMode: Boolean,
    currentLanguage: String,
    themeViewModel: ThemeViewModel,
    context: Context,
    onMenuClick: () -> Unit,
    activity: ComponentActivity,
    onShowLanguageChangeDialog: (String) -> Unit
) {
    val prefs = remember { context.getSharedPreferences(ONBOARDING_PREFS, Context.MODE_PRIVATE) }
    val onboardingCompleted = remember { prefs.getBoolean(ONBOARDING_COMPLETED_KEY, false) }
    val startDestination = if (onboardingCompleted) "category_list" else "onboarding"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("onboarding") {
            OnboardingScreen(
                onFinish = {
                    prefs.edit().putBoolean(ONBOARDING_COMPLETED_KEY, true).apply()
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
                    .filter { it.estado != com.example.taksy.data.TaskEstado.COMPLETADA && it.categoriaId != null }
                    .groupBy { it.categoriaId!! }
                    .mapValues { it.value.size }
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
                onTaskClick = { task ->
                    navController.navigate("task_detail/${task.id}")
                }
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
                onDeleteReminder = { reminder ->
                    taskViewModel.deleteReminder(reminder)
                }
            )
        }

        composable("daily_reminders") {
            DailyReminderScreen(
                onBackClick = {
                    if (!navController.popBackStack()) {
                        navController.navigate("category_list") { launchSingleTop = true }
                    }
                },
                isDarkMode = isDarkMode,
                activity = activity
            )
        }

        composable("tasks_by_category/{categoryId}") { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId")?.toLongOrNull()
            if (categoryId != null) {
                var category by remember { mutableStateOf<com.example.taksy.data.Category?>(null) }
                val tasks by taskViewModel.getTasksByCategoryId(categoryId).collectAsState(initial = emptyList())

                LaunchedEffect(categoryId) {
                    category = categoryViewModel.getCategoryById(categoryId)
                }

                if (category != null) {
                    TasksByCategoryScreen(
                        category = category!!,
                        tasks = tasks,
                        taskViewModel = taskViewModel,
                        onBackClick = { navController.popBackStack() },
                        onTaskClick = { task -> navController.navigate("task_detail/${task.id}") },
                        onTaskToggle = { task -> taskViewModel.toggleTaskStatus(task) },
                        onTaskDelete = { task -> taskViewModel.deleteTask(task) },
                        onAddTask = { title, dueDate, prioridad, recurrencia -> taskViewModel.addTask(title, dueDate, categoryId, prioridad, recurrencia) },
                        showToast = {}
                    )
                }
            }
        }

        composable("theme_settings") {
            ThemeSettingsScreen(
                isDarkMode = isDarkMode,
                onThemeChange = { isDark -> themeViewModel.setDarkMode(isDark) },
                onBackClick = {
                    if (!navController.popBackStack()) {
                        navController.navigate("category_list") { launchSingleTop = true }
                    }
                }
            )
        }

        composable("language_settings") {
            LanguageSettingsScreen(
                currentLanguage = currentLanguage,
                onLanguageChange = { language -> themeViewModel.setLanguage(language) },
                onShowLanguageChangeDialog = onShowLanguageChangeDialog,
                onBackClick = {
                    if (!navController.popBackStack()) {
                        navController.navigate("category_list") { launchSingleTop = true }
                    }
                }
            )
        }

        composable("about") {
            AboutScreen(
                onBackClick = {
                    if (!navController.popBackStack()) {
                        navController.navigate("category_list") { launchSingleTop = true }
                    }
                }
            )
        }

        composable("backup") {
            val backupViewModel: BackupViewModel = hiltViewModel()
            BackupScreen(
                backupViewModel = backupViewModel,
                onBackClick = {
                    if (!navController.popBackStack()) {
                        navController.navigate("category_list") { launchSingleTop = true }
                    }
                }
            )
        }

        composable("task_detail/{taskId}") { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId")?.toLongOrNull()
            if (taskId != null) {
                var task by remember { mutableStateOf<com.example.taksy.data.Task?>(null) }
                val subtasks by taskViewModel.getSubtasksByTaskId(taskId).collectAsState(initial = emptyList())

                LaunchedEffect(taskId) {
                    task = taskViewModel.getTaskById(taskId)
                }

                if (task != null) {
                    TaskDetailScreen(
                        task = task!!,
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

@Composable
fun DrawerScreen(
    navController: androidx.navigation.NavHostController,
    taskViewModel: TaskViewModel,
    categoryViewModel: CategoryViewModel,
    isDarkMode: Boolean,
    currentLanguage: String,
    themeViewModel: ThemeViewModel,
    context: Context,
    activity: ComponentActivity
) {
    var drawerLayout by remember { mutableStateOf<DrawerLayout?>(null) }
    var showLanguageChangeDialog by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val themeRes = R.style.Theme_Ticksy
                val themedContext = androidx.appcompat.view.ContextThemeWrapper(ctx, themeRes)

                DrawerLayout(themedContext).apply {
                    drawerLayout = this
                    layoutParams = DrawerLayout.LayoutParams(
                        DrawerLayout.LayoutParams.MATCH_PARENT,
                        DrawerLayout.LayoutParams.MATCH_PARENT
                    )

                    val contentView = androidx.compose.ui.platform.ComposeView(themedContext).apply {
                        layoutParams = DrawerLayout.LayoutParams(
                            DrawerLayout.LayoutParams.MATCH_PARENT,
                            DrawerLayout.LayoutParams.MATCH_PARENT
                        )
                        setContent {
                            val innerIsDarkMode by themeViewModel.isDarkMode.collectAsState()
                            val innerCurrentLanguage by themeViewModel.currentLanguage.collectAsState()
                            TicksyTheme(darkTheme = innerIsDarkMode) {
                                MainContent(
                                    navController = navController,
                                    taskViewModel = taskViewModel,
                                    categoryViewModel = categoryViewModel,
                                    isDarkMode = innerIsDarkMode,
                                    currentLanguage = innerCurrentLanguage,
                                    themeViewModel = themeViewModel,
                                    context = context,
                                    onMenuClick = { drawerLayout?.openDrawer(GravityCompat.START) },
                                    activity = activity,
                                    onShowLanguageChangeDialog = { language ->
                                        selectedLanguage = language
                                        showLanguageChangeDialog = true
                                    }
                                )
                            }
                        }
                    }
                    addView(contentView)

                    val navigationView = NavigationView(themedContext).apply {
                        layoutParams = DrawerLayout.LayoutParams(
                            DrawerLayout.LayoutParams.WRAP_CONTENT,
                            DrawerLayout.LayoutParams.MATCH_PARENT
                        ).apply {
                            gravity = GravityCompat.START
                            width = (ctx.resources.displayMetrics.widthPixels * 0.75f).toInt()
                        }

                        if (isDarkMode) {
                            setBackgroundColor(android.graphics.Color.parseColor("#121212"))
                            itemTextColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
                            itemIconTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
                        } else {
                            setBackgroundColor(android.graphics.Color.parseColor("#FFFFFF"))
                            itemTextColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.BLACK)
                            itemIconTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.BLACK)
                        }

                        inflateHeaderView(R.layout.drawer_header)
                        inflateMenu(R.menu.drawer_menu)

                        setNavigationItemSelectedListener { menuItem ->
                            drawerLayout?.closeDrawer(GravityCompat.START)
                            val route = when (menuItem.itemId) {
                                R.id.nav_theme -> "theme_settings"
                                R.id.nav_language -> "language_settings"
                                R.id.nav_reminders -> "daily_reminders"
                                R.id.nav_backup -> "backup"
                                R.id.nav_about -> "about"
                                else -> null
                            }
                            route?.let {
                                navController.navigate(it) {
                                    popUpTo("category_list") { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                            true
                        }
                    }
                    addView(navigationView)
                }
            },
            update = { drawer ->
                // Update NavigationView colors when theme changes
                for (i in 0 until drawer.childCount) {
                    val child = drawer.getChildAt(i)
                    if (child is NavigationView) {
                        if (isDarkMode) {
                            child.setBackgroundColor(android.graphics.Color.parseColor("#121212"))
                            child.itemTextColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
                            child.itemIconTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
                        } else {
                            child.setBackgroundColor(android.graphics.Color.parseColor("#FFFFFF"))
                            child.itemTextColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.BLACK)
                            child.itemIconTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.BLACK)
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Diálogo de confirmación para cambio de idioma
        if (showLanguageChangeDialog) {
            AlertDialog(
                onDismissRequest = { showLanguageChangeDialog = false },
                title = { Text(stringResource(R.string.change_language)) },
                text = { Text(stringResource(R.string.change_language_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        themeViewModel.setLanguage(selectedLanguage)
                        showLanguageChangeDialog = false
                    }) { Text(stringResource(R.string.change)) }
                },
                dismissButton = {
                    TextButton(onClick = { showLanguageChangeDialog = false }) { Text(stringResource(R.string.cancel)) }
                }
            )
        }
    }
}
