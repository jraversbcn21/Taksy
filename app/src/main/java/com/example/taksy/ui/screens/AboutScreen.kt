package com.example.taksy.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taksy.R
import com.example.taksy.ui.components.AppIcon
import com.example.taksy.ui.theme.HomePalette
import com.example.taksy.ui.theme.PlusJakartaSans

/**
 * Acerca de: icono, versión (leída del paquete), descripción, características y autor.
 * Mismo lenguaje visual que la pantalla de inicio (HomePalette + Plus Jakarta Sans).
 */
@Composable
fun AboutScreen(
    onBackClick: () -> Unit,
    isDarkMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val palette = if (isDarkMode) HomePalette.Dark else HomePalette.Light
    val context = LocalContext.current
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }
    val features = listOf(
        Triple(Icons.AutoMirrored.Filled.List, R.string.about_feature_categories, Color(0xFF1E88E5)),
        Triple(Icons.Default.CheckCircle, R.string.about_feature_subtasks, Color(0xFF43A047)),
        Triple(Icons.Default.DateRange, R.string.about_feature_recurring, Color(0xFF7E57C2)),
        Triple(Icons.Default.Notifications, R.string.about_feature_reminders, Color(0xFF00897B)),
        Triple(Icons.Default.Lock, R.string.about_feature_backup, Color(0xFFFB8C00)),
        Triple(Icons.Default.Settings, R.string.about_feature_themes, Color(0xFF546E7A)),
        Triple(Icons.Default.Place, R.string.about_feature_languages, Color(0xFFE91E63))
    )

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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AppIcon(size = 56.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.app_name),
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 34.sp,
                            lineHeight = 37.sp,
                            letterSpacing = (-1).sp,
                            color = palette.ink
                        )
                        if (versionName.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.about_version, versionName),
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = palette.muted
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.about_description),
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = palette.muted,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AboutSectionLabel(stringResource(R.string.about_features_title), palette)
                AboutCard(palette) {
                    features.forEachIndexed { index, (icon, label, color) ->
                        if (index > 0) HorizontalDivider(color = palette.border, thickness = 1.dp)
                        AboutRow(icon, color, stringResource(label), palette)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AboutSectionLabel(stringResource(R.string.about_developer), palette)
                AboutCard(palette) {
                    AboutRow(Icons.Default.Person, palette.brand, stringResource(R.string.about_developer_name), palette)
                }
            }

            Text(
                text = stringResource(R.string.about_made_with),
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = palette.muted,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AboutSectionLabel(text: String, palette: HomePalette) {
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
private fun AboutCard(palette: HomePalette, content: @Composable ColumnScope.() -> Unit) {
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
private fun AboutRow(icon: ImageVector, tileColor: Color, text: String, palette: HomePalette) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tileColor)
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
        }
        Text(
            text = text,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = palette.ink
        )
    }
}
