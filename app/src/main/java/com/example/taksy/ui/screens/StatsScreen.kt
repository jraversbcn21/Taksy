package com.example.taksy.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taksy.R
import com.example.taksy.ui.components.PlantCard
import com.example.taksy.ui.theme.DueTodayOrange
import com.example.taksy.ui.theme.HomePalette
import com.example.taksy.ui.theme.PlusJakartaSans
import com.example.taksy.ui.theme.accentColor
import com.example.taksy.utils.CategoryUtils
import com.example.taksy.utils.getCategoryIcon
import com.example.taksy.viewmodel.CategoryStats
import com.example.taksy.viewmodel.DayCompletion
import com.example.taksy.viewmodel.StatsUiState
import com.example.taksy.viewmodel.StatsViewModel
import java.text.SimpleDateFormat

/**
 * Estadísticas: tasa de finalización, racha, contadores, últimos 7 días y categorías principales.
 * Mismo lenguaje visual que la pantalla de inicio (HomePalette + Plus Jakarta Sans).
 */
@Composable
fun StatsScreen(
    onBackClick: () -> Unit,
    isDarkMode: Boolean = false,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = if (isDarkMode) HomePalette.Dark else HomePalette.Light

    Scaffold(containerColor = palette.background) { paddingValues ->
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
                    text = stringResource(R.string.stats_title),
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 34.sp,
                    lineHeight = 37.sp,
                    letterSpacing = (-1).sp,
                    color = palette.ink
                )
                if (state.totalTasks > 0) {
                    Text(
                        text = stringResource(R.string.stats_subtitle, state.completedTasks, state.totalTasks),
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = palette.muted
                    )
                }
            }

            if (state.totalTasks == 0) {
                StatsCard(palette) {
                    Text(
                        text = stringResource(R.string.stats_empty),
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = palette.muted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp)
                    )
                }
                return@Column
            }

            HeroCard(state, palette)
            PlantCard(state.completedTasks, palette)
            CountersRow(state, palette)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(stringResource(R.string.stats_last_7_days), palette)
                WeeklyChart(state.last7Days, palette)
            }

            if (state.topCategories.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel(stringResource(R.string.stats_top_categories), palette)
                    StatsCard(palette) {
                        state.topCategories.forEachIndexed { index, catStats ->
                            if (index > 0) HorizontalDivider(color = palette.border, thickness = 1.dp)
                            CategoryRow(catStats, palette)
                        }
                    }
                }
            }
        }
    }
}

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
private fun StatsCard(
    palette: HomePalette,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.surface)
            .border(1.dp, palette.border, shape),
        content = content
    )
}

/** Anillo con la tasa de finalización + racha de días seguidos con tareas completadas. */
@Composable
private fun HeroCard(state: StatsUiState, palette: HomePalette) {
    val progress by animateFloatAsState(state.completionRate / 100f, tween(600), label = "rate")

    StatsCard(palette) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(vertical = 20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(96.dp)) {
                    Canvas(Modifier.fillMaxSize()) {
                        val stroke = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                        drawArc(palette.track, 0f, 360f, useCenter = false, style = stroke)
                        if (progress > 0f) {
                            drawArc(palette.brand, -90f, 360f * progress, useCenter = false, style = stroke)
                        }
                    }
                    Text(
                        text = "${state.completionRate}%",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        letterSpacing = (-0.5).sp,
                        color = palette.ink
                    )
                }
                HeroLabel(stringResource(R.string.stats_completion_rate), palette)
            }

            VerticalDivider(color = palette.border, thickness = 1.dp)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(DueTodayOrange.copy(alpha = if (palette.isDark) 0.22f else 0.14f))
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = DueTodayOrange, modifier = Modifier.size(28.dp))
                }
                Text(
                    text = pluralStringResource(R.plurals.stats_days, state.streakDays, state.streakDays),
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    letterSpacing = (-0.5).sp,
                    color = palette.ink
                )
                HeroLabel(stringResource(R.string.stats_streak), palette)
            }
        }
    }
}

@Composable
private fun HeroLabel(text: String, palette: HomePalette) {
    Text(
        text = text,
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        color = palette.muted,
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}

@Composable
private fun CountersRow(state: StatsUiState, palette: HomePalette) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.height(IntrinsicSize.Min)
    ) {
        listOf(
            Triple(state.pendingTasks, R.string.stats_pending, palette.brand),
            Triple(state.activeCategoriesCount, R.string.stats_active_categories, if (palette.isDark) Color(0xFF64B5F6) else Color(0xFF1565C0))
        ).forEach { (value, label, color) ->
            CounterTile(
                value = value,
                label = stringResource(label),
                color = color,
                palette = palette,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun CounterTile(value: Int, label: String, color: Color, palette: HomePalette, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clip(shape)
            .background(palette.surface)
            .border(1.dp, palette.border, shape)
            .padding(14.dp)
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.height(4.dp))
        Text(
            text = value.toString(),
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp,
            letterSpacing = (-0.5).sp,
            color = palette.ink
        )
        Text(
            text = label,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            color = palette.muted
        )
    }
}

/** Barras de tareas completadas por día; la de hoy (la última) va resaltada. */
@Composable
private fun WeeklyChart(days: List<DayCompletion>, palette: HomePalette) {
    val locale = LocalConfiguration.current.locales[0]
    // "EEEEE" = inicial del día en el idioma actual (L M X J V S D / M T W T F S S)
    val dayFormat = remember(locale) { SimpleDateFormat("EEEEE", locale) }
    val maxCount = (days.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)

    StatsCard(palette) {
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .padding(horizontal = 12.dp, vertical = 18.dp)
        ) {
            days.forEachIndexed { index, day ->
                val isToday = index == days.lastIndex
                val target = day.count.toFloat() / maxCount
                val fraction by animateFloatAsState(target, tween(500), label = "bar")
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Text(
                        text = if (day.count > 0) day.count.toString() else "",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isToday) palette.ink else palette.muted
                    )
                    Box(
                        contentAlignment = Alignment.BottomCenter,
                        modifier = Modifier
                            .width(22.dp)
                            .weight(1f)
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction)
                                .heightIn(min = 6.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(
                                    when {
                                        day.count == 0 -> palette.track
                                        isToday -> palette.brand
                                        else -> palette.brand.copy(alpha = 0.4f)
                                    }
                                )
                        )
                    }
                    Text(
                        text = dayFormat.format(day.day).uppercase(locale),
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = if (isToday) palette.ink else palette.muted
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(catStats: CategoryStats, palette: HomePalette) {
    val accent = remember(catStats.category.color) { catStats.category.accentColor() }
    val target = if (catStats.total == 0) 0f else catStats.completed.toFloat() / catStats.total
    val progress by animateFloatAsState(target, tween(450), label = "progress")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(accent)
        ) {
            Icon(getCategoryIcon(catStats.category.icono), contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = CategoryUtils.getCategoryName(catStats.category),
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = palette.ink,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.home_done_of, catStats.completed, catStats.total),
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = palette.muted
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(palette.track)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(accent)
                )
            }
        }
    }
}
