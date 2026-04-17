package com.example.taksy.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = IconGreen80,           // Verde medio claro del icono
    secondary = IconYellowGreen80,   // Amarillo-verde claro del icono
    tertiary = IconTeal80,           // Verde-azul claro del icono
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = IconDarkTeal,   // Verde-azul oscuro del icono
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0)
)

private val LightColorScheme = lightColorScheme(
    primary = IconGreen40,           // Verde intenso del icono
    secondary = IconYellowGreen40,   // Amarillo-verde intenso del icono
    tertiary = IconTeal40,           // Verde-azul intenso del icono
    primaryContainer = SplashLightGreen,  // Verde suave del gradiente
    secondaryContainer = SplashLightYellow, // Amarillo-verde suave del gradiente
    tertiaryContainer = SplashLightTeal,   // Azul-verde suave del gradiente
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    surfaceVariant = SplashLightBlue, // Azul suave del gradiente
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onPrimaryContainer = Color(0xFF1C1B1F),
    onSecondaryContainer = Color(0xFF1C1B1F),
    onTertiaryContainer = Color(0xFF1C1B1F),
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

@Composable
fun TicksyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Deshabilitado para usar nuestros colores del icono
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}