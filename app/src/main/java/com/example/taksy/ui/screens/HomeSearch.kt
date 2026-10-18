package com.example.taksy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taksy.R
import com.example.taksy.data.Category
import com.example.taksy.data.Task
import com.example.taksy.data.TaskEstado
import com.example.taksy.data.TaskPrioridad
import com.example.taksy.data.TaskRecurrencia
import com.example.taksy.ui.components.SearchField
import com.example.taksy.ui.components.dueColor
import com.example.taksy.ui.components.priorityColor
import com.example.taksy.ui.theme.HomePalette
import com.example.taksy.ui.theme.PlusJakartaSans
import com.example.taksy.ui.theme.accentColor
import com.example.taksy.utils.CategoryUtils
import com.example.taksy.utils.DateUtils
import com.example.taksy.utils.getCategoryIcon

/**
 * Búsqueda global desde la pantalla de inicio. Sin texto muestra las listas como accesos
 * directos; con texto, los resultados agrupados en pendientes y completadas, con la parte
 * que coincide resaltada. Atrás (BackHandler en CategoryListScreen) la cierra.
 */
@Composable
fun HomeSearch(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    results: List<Task>,
    categories: List<Category>,
    palette: HomePalette,
    onTaskClick: (Task) -> Unit,
    onCategoryClick: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val pending = results.filter { it.estado == TaskEstado.PENDIENTE }
    val completed = results.filter { it.estado == TaskEstado.COMPLETADA }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item(key = "field") {
            SearchField(query, onQueryChange, onClose, palette, accent = palette.brand)
        }

        when {
            query.isBlank() -> {
                item(key = "lists_label") { SearchSectionLabel(stringResource(R.string.home_your_lists), null, palette) }
                item(key = "lists") {
                    CategoryShortcuts(categories, palette, onCategoryClick, Modifier.padding(horizontal = 6.dp))
                }
            }
            results.isEmpty() -> item(key = "empty") { NoResults(palette) }
            else -> {
                if (pending.isNotEmpty()) {
                    item(key = "pending_label") { SearchSectionLabel(stringResource(R.string.tasks_section_pending), pending.size, palette) }
                    items(pending, key = { it.id }) { task ->
                        SearchResultCard(task, task.categoriaId?.let { categoryMap[it] }, query, palette) { onTaskClick(task) }
                    }
                }
                if (completed.isNotEmpty()) {
                    item(key = "done_label") { SearchSectionLabel(stringResource(R.string.tasks_section_done), completed.size, palette) }
                    items(completed, key = { it.id }) { task ->
                        SearchResultCard(task, task.categoriaId?.let { categoryMap[it] }, query, palette) { onTaskClick(task) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSectionLabel(text: String, count: Int?, palette: HomePalette) {
    Text(
        text = if (count != null) "${text.uppercase()} · $count" else text.uppercase(),
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 13.sp,
        letterSpacing = 1.sp,
        color = palette.muted,
        modifier = Modifier.padding(start = 6.dp, top = 16.dp, bottom = 2.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryShortcuts(
    categories: List<Category>,
    palette: HomePalette,
    onCategoryClick: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        categories.forEach { category ->
            val accent = remember(category.color) { category.accentColor() }
            val shape = RoundedCornerShape(16.dp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(shape)
                    .background(palette.surface)
                    .border(1.dp, palette.border, shape)
                    .clickable(role = Role.Button) { onCategoryClick(category) }
                    .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(accent)
                ) {
                    Icon(getCategoryIcon(category.icono), contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = CategoryUtils.getCategoryName(category),
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = palette.ink
                )
            }
        }
    }
}

@Composable
private fun SearchResultCard(task: Task, category: Category?, query: String, palette: HomePalette, onClick: () -> Unit) {
    val isCompleted = task.estado == TaskEstado.COMPLETADA
    val accent = remember(category?.color) { category?.accentColor() ?: palette.brand }
    val shape = RoundedCornerShape(18.dp)
    val title = remember(task.titulo, query, accent) { highlightMatch(task.titulo, query, accent, palette) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isCompleted) palette.doneCard else palette.surface)
            .border(1.dp, palette.border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isCompleted) accent.copy(alpha = 0.45f) else accent)
        ) {
            category?.let {
                Icon(getCategoryIcon(it.icono), contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!isCompleted && task.prioridad != TaskPrioridad.NINGUNA) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(priorityColor(task.prioridad)))
                }
                Text(
                    text = title,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                    color = if (isCompleted) palette.doneText else palette.ink
                )
            }
            SearchResultMeta(task, category, isCompleted, palette, accent)
        }
    }
}

/** "Personal · 17/10/2026": la fecha con el mismo color por cercanía que en la lista de tareas. */
@Composable
private fun SearchResultMeta(task: Task, category: Category?, isCompleted: Boolean, palette: HomePalette, accent: Color) {
    val dateText = if (isCompleted) {
        (task.fechaCompletada ?: task.fechaVencimiento)?.let { stringResource(R.string.completed_on, DateUtils.formatDate(it)) }
    } else {
        task.fechaVencimiento?.let { DateUtils.formatDate(it) }
    }
    val dateColor = if (isCompleted) palette.doneText
    else task.fechaVencimiento?.let { dueColor(DateUtils.getDueDateStatus(it), palette) } ?: palette.muted

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (category != null) {
            Text(
                text = CategoryUtils.getCategoryName(category),
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = palette.muted,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
        if (category != null && dateText != null) {
            Text("·", fontFamily = PlusJakartaSans, fontSize = 12.sp, color = palette.muted)
        }
        if (dateText != null) {
            Text(
                text = dateText,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                maxLines = 1,
                color = dateColor
            )
        }
        if (!isCompleted && task.recurrencia != TaskRecurrencia.NINGUNA) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
        }
    }
}

/** Resalta la primera aparición de [query] en [text] (sin distinguir mayúsculas). */
private fun highlightMatch(text: String, query: String, accent: Color, palette: HomePalette): AnnotatedString {
    val q = query.trim()
    val start = if (q.isEmpty()) -1 else text.indexOf(q, ignoreCase = true)
    if (start < 0) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text.substring(0, start))
        withStyle(SpanStyle(background = accent.copy(alpha = if (palette.isDark) 0.35f else 0.2f), fontWeight = FontWeight.ExtraBold)) {
            append(text.substring(start, start + q.length))
        }
        append(text.substring(start + q.length))
    }
}

@Composable
private fun NoResults(palette: HomePalette) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(palette.track)
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = palette.muted, modifier = Modifier.size(26.dp))
        }
        Text(
            text = stringResource(R.string.no_search_results),
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            color = palette.ink
        )
    }
}
