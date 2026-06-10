package com.example.taksy

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationManagerCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.taksy.ui.navigation.AppNavGraph
import com.example.taksy.ui.theme.TicksyTheme
import com.example.taksy.utils.LocaleHelper
import com.example.taksy.viewmodel.CategoryViewModel
import com.example.taksy.viewmodel.TaskViewModel
import com.example.taksy.viewmodel.ThemeViewModel
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val themeViewModel: ThemeViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* permission result handled silently */ }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (savedInstanceState != null) {
            window.decorView.alpha = 0f
            window.decorView.animate().alpha(1f).setDuration(350).start()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent { MainScreen(activity = this) }
    }
}

@Composable
private fun MainScreen(activity: ComponentActivity) {
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val taskViewModel: TaskViewModel = hiltViewModel()
    val categoryViewModel: CategoryViewModel = hiltViewModel()
    val navController = rememberNavController()

    val isDarkMode by themeViewModel.isDarkMode.collectAsStateWithLifecycle()
    val currentLanguage by themeViewModel.currentLanguage.collectAsStateWithLifecycle()

    TicksyTheme(darkTheme = isDarkMode) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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

@Composable
private fun DrawerScreen(
    navController: NavHostController,
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
                val themedContext = androidx.appcompat.view.ContextThemeWrapper(ctx, R.style.Theme_Ticksy)

                DrawerLayout(themedContext).apply {
                    drawerLayout = this
                    layoutParams = DrawerLayout.LayoutParams(
                        DrawerLayout.LayoutParams.MATCH_PARENT,
                        DrawerLayout.LayoutParams.MATCH_PARENT
                    )

                    val contentView = ComposeView(themedContext).apply {
                        layoutParams = DrawerLayout.LayoutParams(
                            DrawerLayout.LayoutParams.MATCH_PARENT,
                            DrawerLayout.LayoutParams.MATCH_PARENT
                        )
                        setContent {
                            val innerIsDarkMode by themeViewModel.isDarkMode.collectAsState()
                            val innerCurrentLanguage by themeViewModel.currentLanguage.collectAsState()
                            TicksyTheme(darkTheme = innerIsDarkMode) {
                                AppNavGraph(
                                    navController = navController,
                                    taskViewModel = taskViewModel,
                                    categoryViewModel = categoryViewModel,
                                    themeViewModel = themeViewModel,
                                    isDarkMode = innerIsDarkMode,
                                    currentLanguage = innerCurrentLanguage,
                                    context = context,
                                    activity = activity,
                                    onMenuClick = { drawerLayout?.openDrawer(GravityCompat.START) },
                                    onShowLanguageChangeDialog = { language ->
                                        selectedLanguage = language
                                        showLanguageChangeDialog = true
                                    }
                                )
                            }
                        }
                    }
                    addView(contentView)
                    addView(buildNavigationView(themedContext, isDarkMode, navController) {
                        drawerLayout?.closeDrawer(GravityCompat.START)
                    })
                }
            },
            update = { drawer -> applyDrawerTheme(drawer, isDarkMode) },
            modifier = Modifier.fillMaxSize()
        )

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

private fun buildNavigationView(
    themedContext: Context,
    isDarkMode: Boolean,
    navController: NavHostController,
    onItemSelected: () -> Unit
): NavigationView = NavigationView(themedContext).apply {
    layoutParams = DrawerLayout.LayoutParams(
        DrawerLayout.LayoutParams.WRAP_CONTENT,
        DrawerLayout.LayoutParams.MATCH_PARENT
    ).apply {
        gravity = GravityCompat.START
        width = (themedContext.resources.displayMetrics.widthPixels * 0.75f).toInt()
    }

    applyNavigationViewColors(this, isDarkMode)

    inflateHeaderView(R.layout.drawer_header)
    inflateMenu(R.menu.drawer_menu)

    setNavigationItemSelectedListener { menuItem ->
        onItemSelected()
        val route = when (menuItem.itemId) {
            R.id.nav_theme -> "theme_settings"
            R.id.nav_language -> "language_settings"
            R.id.nav_reminders -> "daily_reminders"
            R.id.nav_stats -> "stats"
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

private fun applyDrawerTheme(drawer: DrawerLayout, isDarkMode: Boolean) {
    for (i in 0 until drawer.childCount) {
        val child = drawer.getChildAt(i)
        if (child is NavigationView) applyNavigationViewColors(child, isDarkMode)
    }
}

private fun applyNavigationViewColors(view: NavigationView, isDarkMode: Boolean) {
    if (isDarkMode) {
        view.setBackgroundColor(android.graphics.Color.parseColor("#121212"))
        view.itemTextColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
        view.itemIconTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
    } else {
        view.setBackgroundColor(android.graphics.Color.parseColor("#FFFFFF"))
        view.itemTextColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.BLACK)
        view.itemIconTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.BLACK)
    }
}
