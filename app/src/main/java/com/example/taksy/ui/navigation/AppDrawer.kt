package com.example.taksy.ui.navigation

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavHostController
import com.example.taksy.R
import com.example.taksy.ui.theme.TicksyTheme
import com.example.taksy.ui.theme.isDark
import com.example.taksy.viewmodel.CategoryViewModel
import com.example.taksy.viewmodel.TaskViewModel
import com.example.taksy.viewmodel.ThemeViewModel
import com.google.android.material.navigation.NavigationView

/**
 * Menú lateral: un DrawerLayout de Android (no un drawer de Compose) con el NavHost dentro
 * de un ComposeView y un NavigationView con los destinos del menú. También muestra el
 * diálogo de confirmación del cambio de idioma, que recrea la actividad.
 */
@Composable
fun DrawerScreen(
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
                            val innerThemeMode by themeViewModel.themeMode.collectAsState()
                            val innerIsDarkMode = innerThemeMode.isDark()
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
            R.id.nav_categories -> "category_management"
            R.id.nav_settings -> "settings"
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
