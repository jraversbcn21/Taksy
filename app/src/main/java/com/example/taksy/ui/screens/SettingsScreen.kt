package com.example.taksy.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taksy.R
import com.example.taksy.data.preferences.ThemeMode
import com.example.taksy.service.DailyReminderManager
import com.example.taksy.ui.theme.HomePalette
import com.example.taksy.ui.theme.PlusJakartaSans
import java.util.Locale

/**
 * Ajustes de la app en una sola pantalla: tema, idioma y acceso a los recordatorios diarios.
 * El cambio de idioma pasa por el diálogo de confirmación de MainActivity porque recrea la actividad.
 * Mismo lenguaje visual que la pantalla de inicio (HomePalette + Plus Jakarta Sans).
 */
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    currentLanguage: String,
    onThemeModeChange: (ThemeMode) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onDailyRemindersClick: () -> Unit,
    onBackClick: () -> Unit,
    isDarkMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val palette = if (isDarkMode) HomePalette.Dark else HomePalette.Light
    val context = LocalContext.current
    // Se recalcula al volver de la pantalla de recordatorios (la composición se recrea)
    val reminderPrefs = remember { DailyReminderManager.loadPrefs(context) }
    val reminderSummary = if (reminderPrefs.enabled) {
        stringResource(
            R.string.settings_reminders_on,
            formatTime(reminderPrefs.morningHour, reminderPrefs.morningMinute),
            formatTime(reminderPrefs.eveningHour, reminderPrefs.eveningMinute)
        )
    } else {
        stringResource(R.string.settings_reminders_off)
    }

    Scaffold(containerColor = palette.background, modifier = modifier) { paddingValues ->
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 32.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                IconButton(onClick = onBackClick, modifier = Modifier.offset(x = (-12).dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = palette.ink
                    )
                }
                Text(
                    text = stringResource(R.string.settings),
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 34.sp,
                    lineHeight = 37.sp,
                    letterSpacing = (-1).sp,
                    color = palette.ink
                )
            }

            // ── Tema ──
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(stringResource(R.string.theme), palette)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        ThemeMode.SYSTEM to R.string.theme_system,
                        ThemeMode.LIGHT to R.string.theme_light,
                        ThemeMode.DARK to R.string.theme_dark
                    ).forEach { (mode, label) ->
                        ThemeTile(
                            mode = mode,
                            label = stringResource(label),
                            selected = themeMode == mode,
                            palette = palette,
                            onClick = { onThemeModeChange(mode) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── Idioma ──
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(stringResource(R.string.language), palette)
                SettingsCard(palette) {
                    listOf(
                        "es" to R.string.language_spanish,
                        "en" to R.string.language_english
                    ).forEachIndexed { index, (language, label) ->
                        if (index > 0) HorizontalDivider(color = palette.border, thickness = 1.dp)
                        LanguageRow(
                            code = language.uppercase(Locale.ROOT),
                            label = stringResource(label),
                            selected = currentLanguage == language,
                            palette = palette,
                            onClick = { onLanguageSelected(language) }
                        )
                    }
                }
            }

            // ── Notificaciones ──
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(stringResource(R.string.settings_section_notifications), palette)
                SettingsCard(palette) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onDailyRemindersClick)
                            .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 14.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF00897B))
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.daily_reminders),
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = palette.ink
                            )
                            Text(
                                text = reminderSummary,
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = palette.muted
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = palette.muted
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(hour: Int, minute: Int) = String.format(Locale.ROOT, "%02d:%02d", hour, minute)

@Composable
private fun SectionLabel(text: String, palette: HomePalette) {
    Text(
        text = text.uppercase(),
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 13.sp,
        letterSpacing = 1.sp,
        color = palette.muted
    )
}

@Composable
private fun SettingsCard(palette: HomePalette, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.surface)
            .border(1.dp, palette.border, shape),
        content = content
    )
}

@Composable
private fun ThemeTile(
    mode: ThemeMode,
    label: String,
    selected: Boolean,
    palette: HomePalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ring by animateColorAsState(if (selected) palette.brand else Color.Transparent, label = "ring")
    val lightBg = Color.White
    val darkBg = Color(0xFF1E1E1E)
    val barColor = when (mode) {
        ThemeMode.DARK -> Color(0xFF3A403C)
        ThemeMode.SYSTEM -> Color(0xFF9AA49E)
        ThemeMode.LIGHT -> Color(0xFFE3E7DF)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .border(2.dp, ring, RoundedCornerShape(16.dp))
                .padding(5.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (mode == ThemeMode.DARK) darkBg else lightBg)
                .drawBehind {
                    if (mode == ThemeMode.SYSTEM) {
                        val half = Path().apply {
                            moveTo(size.width, 0f)
                            lineTo(size.width, size.height)
                            lineTo(0f, size.height)
                            close()
                        }
                        drawPath(half, darkBg)
                    }
                }
                .padding(9.dp)
        ) {
            Box(Modifier.size(14.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFF4CAF50)))
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 22.dp)
            ) {
                Box(Modifier.fillMaxWidth(0.85f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(barColor))
                Box(Modifier.fillMaxWidth(0.6f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(barColor))
            }
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .size(width = 30.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF689F38), Color(0xFF00897B))))
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (selected) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = palette.brand, modifier = Modifier.size(15.dp))
            }
            Text(
                text = label,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (selected) palette.brand else palette.muted
            )
        }
    }
}

@Composable
private fun LanguageRow(
    code: String,
    label: String,
    selected: Boolean,
    palette: HomePalette,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.brand.copy(alpha = if (palette.isDark) 0.14f else 0.1f))
        ) {
            Text(code, fontFamily = PlusJakartaSans, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = palette.brand)
        }
        Text(
            text = label,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = palette.ink,
            modifier = Modifier.weight(1f)
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (selected) palette.brand else Color.Transparent)
                .then(if (selected) Modifier else Modifier.border(2.dp, palette.border, CircleShape))
        ) {
            if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
        }
    }
}
