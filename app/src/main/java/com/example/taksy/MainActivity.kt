package com.example.taksy

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.example.taksy.ui.navigation.DrawerScreen
import com.example.taksy.ui.theme.TicksyTheme
import com.example.taksy.ui.theme.isDark
import com.example.taksy.utils.LocaleHelper
import com.example.taksy.viewmodel.CategoryViewModel
import com.example.taksy.viewmodel.TaskViewModel
import com.example.taksy.viewmodel.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.merge

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

    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()

    // Los ViewModels no pueden mostrar UI: sus errores llegan aquí como eventos y se muestran como Toast
    val context = LocalContext.current
    LaunchedEffect(taskViewModel, categoryViewModel) {
        merge(taskViewModel.errors, categoryViewModel.errors).collect { message ->
            Toast.makeText(context, context.getString(message), Toast.LENGTH_SHORT).show()
        }
    }
    val isDarkMode = themeMode.isDark()

    // Las barras del sistema siguen el tema de la app, no el del sistema: si no, con la app
    // en oscuro y el móvil en claro, los iconos de la barra de estado quedan negro sobre negro.
    DisposableEffect(isDarkMode) {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ) { isDarkMode },
            navigationBarStyle = SystemBarStyle.auto(LightNavigationScrim, DarkNavigationScrim) { isDarkMode }
        )
        onDispose {}
    }
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

// Mismos scrims que usa enableEdgeToEdge() por defecto
private val LightNavigationScrim = android.graphics.Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DarkNavigationScrim = android.graphics.Color.argb(0x80, 0x1b, 0x1b, 0x1b)
