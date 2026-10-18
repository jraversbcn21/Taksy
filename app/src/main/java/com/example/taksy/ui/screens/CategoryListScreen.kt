package com.example.taksy.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.taksy.R
import com.example.taksy.ui.components.AppIcon
import com.example.taksy.data.Category
import com.example.taksy.data.Task
import com.example.taksy.data.TaskEstado
import com.example.taksy.data.TaskPrioridad
import com.example.taksy.data.TaskRecurrencia
import com.example.taksy.ui.theme.DueOverdueRed
import com.example.taksy.ui.theme.DueTodayOrange
import com.example.taksy.ui.theme.DueTomorrowOrange
import com.example.taksy.ui.theme.HomePalette
import com.example.taksy.ui.theme.accentColor
import com.example.taksy.ui.theme.PlusJakartaSans
import com.example.taksy.ui.theme.PriorityHighRed
import com.example.taksy.ui.theme.PriorityLowGreen
import com.example.taksy.ui.theme.PriorityMediumOrange
import com.example.taksy.utils.CategoryUtils
import com.example.taksy.utils.DateUtils
import com.example.taksy.utils.DueDateStatus
import com.example.taksy.utils.GreetingUtils
import com.example.taksy.utils.getCategoryIcon
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Pantalla principal: saludo según la hora, tareas de hoy y listas (categorías) con progreso.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CategoryListScreen(
    categories: List<Category>,
    taskCountByCategory: Map<Long, Int> = emptyMap(),
    totalTaskCountByCategory: Map<Long, Int> = emptyMap(),
    todayTasks: List<Task> = emptyList(),
    isDarkMode: Boolean = false,
    currentLanguage: String = "es",
    onSettingsClick: () -> Unit = {},
    onCategoryClick: (Category) -> Unit = {},
    onEditCategoryClick: (Category) -> Unit = {},
    onDeleteCategoryClick: (Category) -> Unit = {},
    onReorderCategories: (List<Category>) -> Unit = {},
    showToast: (String) -> Unit = {},
    searchResults: List<Task> = emptyList(),
    onSearchQueryChanged: (String) -> Unit = {},
    onTaskClick: (Task) -> Unit = {},
    onToggleTask: (Task) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = if (isDarkMode) HomePalette.Dark else HomePalette.Light

    var categoriesState by remember { mutableStateOf(categories) }
    val listState = rememberLazyListState()
    // Reordenar listas: mantener pulsada una fila la levanta y entonces se puede arrastrar.
    // Un deslizamiento normal hace scroll de la pantalla.
    var draggedKey by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val haptics = LocalHapticFeedback.current
    val baseViewConfiguration = LocalViewConfiguration.current
    val reorderViewConfiguration = remember(baseViewConfiguration) {
        object : ViewConfiguration by baseViewConfiguration {
            override val longPressTimeoutMillis = REORDER_LONG_PRESS_MILLIS
        }
    }

    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(categories) { if (draggedKey == null) categoriesState = categories }
    val categoryMap = remember(categories) { categories.associateBy { it.id } }

    // dragOffset es el desplazamiento de la fila arrastrada respecto a su posición en la lista
    fun dragBy(delta: Float) {
        val key = draggedKey ?: return
        dragOffset += delta
        val layoutInfo = listState.layoutInfo
        val current = layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } ?: return

        // Cerca del borde de la pantalla la lista hace scroll sola; la fila sigue bajo el dedo
        val top = current.offset + dragOffset
        val edge = current.size / 2f
        val scrollDelta = when {
            delta > 0 && top + current.size > layoutInfo.viewportEndOffset - edge -> delta
            delta < 0 && top < layoutInfo.viewportStartOffset + edge -> delta
            else -> 0f
        }
        if (scrollDelta != 0f) dragOffset += listState.dispatchRawDelta(scrollDelta)

        val center = (current.offset + dragOffset + current.size / 2f).toInt()
        val target = layoutInfo.visibleItemsInfo.firstOrNull {
            it.key is Long && it.key != key && center in it.offset until it.offset + it.size
        } ?: return
        val from = categoriesState.indexOfFirst { it.id == key }
        val to = categoriesState.indexOfFirst { it.id == target.key }
        if (from < 0 || to < 0) return
        categoriesState = categoriesState.toMutableList().apply { add(to, removeAt(from)) }
        dragOffset += current.offset - target.offset
    }

    // El orden se guarda una sola vez, al soltar
    fun endDrag() {
        if (draggedKey == null) return
        draggedKey = null
        dragOffset = 0f
        if (categoriesState.map { it.id } != categories.map { it.id }) {
            val reordered = categoriesState.mapIndexed { index, category -> category.copy(orden = index + 1) }
            categoriesState = reordered
            onReorderCategories(reordered)
        }
    }

    fun closeSearch() {
        showSearch = false
        searchQuery = ""
        onSearchQueryChanged("")
    }
    // Atrás cierra la búsqueda en vez de salir de la app
    BackHandler(enabled = showSearch, onBack = ::closeSearch)

    Scaffold(
        containerColor = palette.background,
        modifier = modifier
    ) { paddingValues ->
        if (showSearch) {
            HomeSearch(
                query = searchQuery,
                onQueryChange = {
                    searchQuery = it
                    onSearchQueryChanged(it)
                },
                onClose = ::closeSearch,
                results = searchResults,
                categories = categories,
                palette = palette,
                onTaskClick = onTaskClick,
                onCategoryClick = onCategoryClick,
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
            ) {
                item(key = "header") {
                    HomeHeader(palette, onMenuClick = onSettingsClick, modifier = Modifier.padding(horizontal = 22.dp))
                }
                item(key = "greeting") {
                    HomeGreeting(
                        todayTasks = todayTasks,
                        palette = palette,
                        modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 26.dp)
                    )
                }
                item(key = "search") {
                    SearchPill(
                        palette = palette,
                        onClick = { showSearch = true },
                        modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 20.dp)
                    )
                }
                if (todayTasks.isNotEmpty()) {
                    item(key = "today") {
                        val now by rememberNow()
                        LazyRow(
                            modifier = Modifier.padding(top = 20.dp),
                            contentPadding = PaddingValues(horizontal = 22.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(todayTasks, key = { it.id }) { task ->
                                TodayTaskCard(
                                    task = task,
                                    category = task.categoriaId?.let { categoryMap[it] },
                                    palette = palette,
                                    now = now,
                                    onClick = { onTaskClick(task) },
                                    onToggle = { onToggleTask(task) },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
                item(key = "lists_title") {
                    Text(
                        text = stringResource(R.string.home_your_lists).uppercase(),
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp,
                        color = palette.muted,
                        modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 24.dp, bottom = 6.dp)
                    )
                }
                items(items = categoriesState, key = { it.id }) { category ->
                    val isDragged = draggedKey == category.id
                    val pending = taskCountByCategory[category.id] ?: 0
                    val total = maxOf(totalTaskCountByCategory[category.id] ?: 0, pending)
                    CompositionLocalProvider(LocalViewConfiguration provides reorderViewConfiguration) {
                        HomeCategoryRow(
                            category = category,
                            doneCount = total - pending,
                            totalCount = total,
                            palette = palette,
                            isDragging = isDragged,
                            // Al soltar tras mantener pulsado no se abre la lista
                            onClick = { if (draggedKey == null) onCategoryClick(category) },
                            modifier = Modifier
                                .then(
                                    if (isDragged) Modifier.zIndex(1f).graphicsLayer { translationY = dragOffset }
                                    else Modifier.animateItem()
                                )
                                .padding(horizontal = 14.dp)
                                .pointerInput(category.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            draggedKey = category.id
                                            dragOffset = 0f
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            dragBy(amount.y)
                                        },
                                        onDragEnd = ::endDrag,
                                        onDragCancel = ::endDrag
                                    )
                                }
                        )
                    }
                }
            }
        }
    }
}

// ── Cabecera ─────────────────────────────────────────────────────────────

@Composable
private fun HomeHeader(palette: HomePalette, onMenuClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        AppIcon(size = 26.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.app_name),
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = palette.brand,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onMenuClick) {
            Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.settings), tint = palette.ink)
        }
    }
}

/** Hora actual que se actualiza al cambiar de minuto (para el saludo y la fecha). */
@Composable
private fun rememberNow(): State<Calendar> = produceState(initialValue = Calendar.getInstance()) {
    while (true) {
        delay(60_000L - System.currentTimeMillis() % 60_000L)
        value = Calendar.getInstance()
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun HomeGreeting(todayTasks: List<Task>, palette: HomePalette, modifier: Modifier = Modifier) {
    val now by rememberNow()
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val datePattern = stringResource(R.string.home_date_pattern)
    val dayKey = now.get(Calendar.YEAR) * 1000 + now.get(Calendar.DAY_OF_YEAR)
    val dateText = remember(dayKey, datePattern, locale) {
        SimpleDateFormat(datePattern, locale).format(now.time).replaceFirstChar { it.titlecase(locale) }
    }
    val greeting = stringResource(GreetingUtils.greetingRes(now.get(Calendar.HOUR_OF_DAY)))
    val pendingToday = todayTasks.count { it.estado == TaskEstado.PENDIENTE }
    val headline = when {
        todayTasks.isEmpty() -> stringResource(R.string.home_today_free)
        pendingToday == 0 -> stringResource(R.string.home_today_done)
        else -> pluralStringResource(R.plurals.home_today_left, pendingToday, pendingToday)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        Text(
            text = dateText,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = palette.muted
        )
        Text(
            text = buildAnnotatedString {
                append(greeting)
                append("\n")
                withStyle(SpanStyle(brush = Brush.horizontalGradient(palette.headlineGradient))) {
                    append(headline)
                }
            },
            style = TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 34.sp,
                lineHeight = 37.sp,
                letterSpacing = (-1).sp,
                color = palette.ink
            )
        )
    }
}

@Composable
private fun SearchPill(palette: HomePalette, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(shape)
            .background(palette.surface)
            .border(1.dp, palette.border, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp)
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = palette.placeholder, modifier = Modifier.size(20.dp))
        Text(
            text = stringResource(R.string.search_tasks),
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = palette.placeholder
        )
    }
}

// ── Tareas de hoy ────────────────────────────────────────────────────────

@Composable
private fun TodayTaskCard(
    task: Task,
    category: Category?,
    palette: HomePalette,
    now: Calendar,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val done = task.estado == TaskEstado.COMPLETADA
    // Una ocurrencia recurrente completada ya generó la siguiente: reabrirla dejaría dos pendientes
    val canToggle = !(done && task.recurrencia != TaskRecurrencia.NINGUNA)
    val color = category?.accentColor() ?: palette.brand
    val accentText = if (palette.isDark) lerp(color, Color.White, 0.4f) else lerp(color, Color.Black, 0.3f)
    val shape = RoundedCornerShape(20.dp)
    val bg = when {
        done -> palette.doneCard
        palette.isDark -> color.copy(alpha = 0.12f)
        else -> color.copy(alpha = 0.08f)
    }
    // Hora de vencimiento; las 00:00 se toman como "sin hora"
    val dueTime = task.fechaVencimiento?.let { due ->
        Calendar.getInstance().apply { time = due }
            .takeUnless { it.get(Calendar.HOUR_OF_DAY) == 0 && it.get(Calendar.MINUTE) == 0 }
    }
    // Vencida: pendiente y con la hora ya pasada (se vuelve a comprobar cada minuto)
    val overdue = !done && dueTime != null && dueTime.timeInMillis < now.timeInMillis
    val overdueColor = if (palette.isDark) lerp(DueOverdueRed, Color.White, 0.25f) else DueOverdueRed
    val borderColor = when {
        done -> palette.border
        overdue -> overdueColor.copy(alpha = 0.5f)
        palette.isDark -> color.copy(alpha = 0.25f)
        else -> color.copy(alpha = 0.2f)
    }
    val timeLabel = when {
        done -> stringResource(R.string.home_task_done)
        dueTime != null -> String.format(
            Locale.ROOT, "%02d:%02d", dueTime.get(Calendar.HOUR_OF_DAY), dueTime.get(Calendar.MINUTE)
        )
        else -> stringResource(R.string.home_today_label)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .width(150.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = timeLabel,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (overdue) overdueColor else accentText,
                modifier = Modifier.weight(1f)
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(enabled = canToggle, role = Role.Checkbox, onClick = onToggle)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (done) color else Color.Transparent)
                        .then(if (done) Modifier else Modifier.border(2.dp, color, CircleShape))
                ) {
                    if (done) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = stringResource(R.string.home_mark_done),
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Text(
                text = task.titulo,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                // Siempre 2 líneas: todas las tarjetas de hoy miden lo mismo
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (done) palette.doneText else palette.ink,
                textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None
            )
            if (category != null) {
                Text(
                    text = CategoryUtils.getCategoryName(category),
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = palette.muted
                )
            }
        }
    }
}

// ── Listas (categorías) ──────────────────────────────────────────────────

@Composable
private fun HomeCategoryRow(
    category: Category,
    doneCount: Int,
    totalCount: Int,
    palette: HomePalette,
    isDragging: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = category.accentColor()
    val scale by animateFloatAsState(if (isDragging) 1.04f else 1f, tween(200), label = "scale")
    val elevation by animateDpAsState(if (isDragging) 10.dp else 0.dp, tween(200), label = "elevation")
    val target = if (totalCount > 0) doneCount.toFloat() / totalCount else 0f
    val progress by animateFloatAsState(target, tween(450), label = "progress")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(elevation, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDragging) palette.surface else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        CategoryIconTile(category, color, palette, size = 44)
        Column(verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = CategoryUtils.getCategoryName(category),
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = palette.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (totalCount == 0) stringResource(R.string.home_no_tasks)
                    else stringResource(R.string.home_done_of, doneCount, totalCount),
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = palette.muted
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(palette.track)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(color)
                )
            }
        }
    }
}

@Composable
private fun CategoryIconTile(category: Category, color: Color, palette: HomePalette, size: Int) {
    val shape = RoundedCornerShape((size * 0.32f).dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size.dp)
            .then(
                if (palette.isDark) Modifier
                else Modifier.shadow(6.dp, shape, ambientColor = color, spotColor = color)
            )
            .clip(shape)
            .background(color)
    ) {
        Icon(
            getCategoryIcon(category.icono),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size((size / 2).dp)
        )
    }
}

/** Tiempo que hay que mantener pulsada una lista para poder moverla. */
private const val REORDER_LONG_PRESS_MILLIS = 1000L
