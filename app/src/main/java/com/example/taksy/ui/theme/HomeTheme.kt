package com.example.taksy.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.taksy.R
import com.example.taksy.data.Category

/** Plus Jakarta Sans — ficheros .ttf en res/font (ver LEEME.md). */
val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold)
)

/** Colores de la pantalla de inicio (diseño 1b / 2a). */
@Immutable
data class HomePalette(
    val background: Color,
    val surface: Color,
    val ink: Color,
    val muted: Color,
    val placeholder: Color,
    val border: Color,
    val track: Color,
    val pressed: Color,
    val doneCard: Color,
    val doneText: Color,
    val brand: Color,
    val headlineGradient: List<Color>,
    val isDark: Boolean
) {
    companion object {
        val Light = HomePalette(
            background = Color(0xFFFBFAF6),
            surface = Color(0xFFFFFFFF),
            ink = Color(0xFF1A1F1C),
            muted = Color(0xFF6A746E),
            placeholder = Color(0xFF7C857F),
            border = Color(0xFFE7EAE3),
            track = Color(0xFFECEEE8),
            pressed = Color(0xFFF1F3EC),
            doneCard = Color(0xFFF1F3EC),
            doneText = Color(0xFF9AA49E),
            brand = IconAccentDark,
            headlineGradient = listOf(IconYellowGreen40, Color(0xFF00897B), Color(0xFF1565C0)),
            isDark = false
        )
        val Dark = HomePalette(
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            ink = Color(0xFFE6E8E6),
            muted = Color(0xFF9DA6A0),
            placeholder = Color(0xFF8E9792),
            border = Color(0xFF2C312E),
            track = Color(0xFF2C312E),
            pressed = Color(0xFF1E1E1E),
            doneCard = Color(0xFF1A1A1A),
            doneText = Color(0xFF6E7772),
            brand = IconGreen80,
            headlineGradient = listOf(IconLightYellow, IconLightTeal, Color(0xFF64B5F6)),
            isDark = true
        )
    }
}

/** Color de una categoría (hex guardado en `Category.color`) como Color de Compose. */
fun Category.accentColor(): Color = Color(android.graphics.Color.parseColor(color))
