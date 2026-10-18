package com.example.taksy.ui.navigation

import android.content.Context
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.taksy.R
import com.example.taksy.ui.components.DialogButtons
import com.example.taksy.ui.components.DialogTitle
import com.example.taksy.ui.components.HomeDialogSurface
import com.example.taksy.ui.theme.HomePalette
import com.example.taksy.ui.theme.PlusJakartaSans
import com.example.taksy.ui.theme.TicksyTheme
import com.example.taksy.ui.theme.isDark
import com.example.taksy.viewmodel.CategoryViewModel
import com.example.taksy.viewmodel.TaskViewModel
import com.example.taksy.viewmodel.ThemeViewModel

/**
 * Menú lateral: un DrawerLayout de Android (no un drawer de Compose) con el NavHost en un
 * ComposeView y el panel del menú en otro ComposeView, dibujado con HomePalette. Bloqueado
 * durante el onboarding. También muestra el diálogo de confirmación del cambio de idioma,
 * que recrea la actividad.
 */
@Composable
fun DrawerScreen(
    navController: NavHostController,
    taskViewModel: TaskViewModel,
    categoryViewModel: CategoryViewModel,
    isDarkMode: Boolean,
    currentLanguage: String,
    themeViewModel: ThemeViewModel,
    context: Context
) {
    var drawerLayout by remember { mutableStateOf<DrawerLayout?>(null) }
    var drawerOpen by remember { mutableStateOf(false) }
    var pendingLanguage by remember { mutableStateOf<String?>(null) }
    val palette = if (isDarkMode) HomePalette.Dark else HomePalette.Light

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    LaunchedEffect(drawerLayout, currentRoute) {
        drawerLayout?.setDrawerLockMode(
            if (currentRoute == "onboarding") DrawerLayout.LOCK_MODE_LOCKED_CLOSED else DrawerLayout.LOCK_MODE_UNLOCKED
        )
    }
    BackHandler(enabled = drawerOpen) { drawerLayout?.closeDrawer(GravityCompat.START) }

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
                    setScrimColor(0x66000000)
                    addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
                        override fun onDrawerOpened(drawerView: View) { drawerOpen = true }
                        override fun onDrawerClosed(drawerView: View) { drawerOpen = false }
                    })

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
                                    onMenuClick = { drawerLayout?.openDrawer(GravityCompat.START) },
                                    onShowLanguageChangeDialog = { language -> pendingLanguage = language }
                                )
                            }
                        }
                    }
                    addView(contentView)

                    val panelView = ComposeView(themedContext).apply {
                        layoutParams = DrawerLayout.LayoutParams(
                            (themedContext.resources.displayMetrics.widthPixels * 0.8f).toInt(),
                            DrawerLayout.LayoutParams.MATCH_PARENT
                        ).apply { gravity = GravityCompat.START }
                        setContent {
                            val panelThemeMode by themeViewModel.themeMode.collectAsState()
                            DrawerPanel(
                                palette = if (panelThemeMode.isDark()) HomePalette.Dark else HomePalette.Light,
                                onNavigate = { route ->
                                    drawerLayout?.closeDrawer(GravityCompat.START)
                                    navController.navigate(route) {
                                        popUpTo("category_list") { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                    }
                    addView(panelView)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        pendingLanguage?.let { language ->
            Dialog(onDismissRequest = { pendingLanguage = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                HomeDialogSurface(palette) {
                    DialogTitle(stringResource(R.string.change_language), palette)
                    Text(
                        text = stringResource(R.string.change_language_message),
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        color = palette.muted
                    )
                    DialogButtons(
                        confirmText = stringResource(R.string.change),
                        confirmColor = palette.brand,
                        palette = palette,
                        onDismiss = { pendingLanguage = null },
                        onConfirm = {
                            pendingLanguage = null
                            themeViewModel.setLanguage(language)
                        }
                    )
                }
            }
        }
    }
}

private data class DrawerItem(val route: String, val labelRes: Int, val color: Color)

private val drawerItems = listOf(
    DrawerItem("category_management", R.string.manage_categories, Color(0xFF1E88E5)),
    DrawerItem("stats", R.string.stats_title, Color(0xFFFB8C00)),
    DrawerItem("backup", R.string.backup, Color(0xFF00897B)),
    DrawerItem("settings", R.string.settings, Color(0xFF546E7A)),
    DrawerItem("about", R.string.about, Color(0xFFE91E63))
)

@Composable
private fun drawerIcon(route: String): Painter = when (route) {
    "category_management" -> painterResource(R.drawable.ic_category)
    "stats" -> painterResource(R.drawable.ic_stats)
    "backup" -> rememberVectorPainter(Icons.Default.Lock)
    "settings" -> painterResource(R.drawable.ic_settings)
    else -> painterResource(R.drawable.ic_info)
}

/** Panel del menú lateral: logo, versión y los destinos, con el lenguaje visual de la pantalla de inicio. */
@Composable
private fun DrawerPanel(palette: HomePalette, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 14.dp, vertical = 20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(start = 8.dp, bottom = 24.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ticksy_icon),
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
            )
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    letterSpacing = (-0.5).sp,
                    color = palette.ink
                )
                if (versionName.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.about_version, versionName),
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = palette.muted
                    )
                }
            }
        }

        drawerItems.forEach { item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(role = Role.Button) { onNavigate(item.route) }
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(item.color)
                ) {
                    Icon(drawerIcon(item.route), contentDescription = null, tint = Color.White, modifier = Modifier.size(21.dp))
                }
                Text(
                    text = stringResource(item.labelRes),
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = palette.ink
                )
            }
        }
    }
}
