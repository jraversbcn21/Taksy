package com.example.taksy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taksy.R
import com.example.taksy.data.Category
import com.example.taksy.ui.components.AddCategoryDialog
import com.example.taksy.ui.components.DeleteCategoryDialog
import com.example.taksy.ui.theme.HomePalette
import com.example.taksy.ui.theme.PlusJakartaSans
import com.example.taksy.ui.theme.accentColor
import com.example.taksy.utils.CategoryUtils
import com.example.taksy.utils.getCategoryIcon

/**
 * Gestión de categorías: crear (botón "Añadir categoría"), editar tocando una fila y
 * eliminar con la papelera (avisa de cuántas tareas se borran con ella).
 * Mismo lenguaje visual que la pantalla de inicio (HomePalette + Plus Jakarta Sans).
 */
@Composable
fun CategoryManagementScreen(
    categories: List<Category>,
    taskCountByCategory: Map<Long, Int>,
    onBackClick: () -> Unit,
    onAddCategory: (String, String, String) -> Unit,
    onUpdateCategory: (Category) -> Unit,
    onDeleteCategory: (Category) -> Unit,
    showToast: (String) -> Unit,
    isDarkMode: Boolean = false
) {
    val palette = if (isDarkMode) HomePalette.Dark else HomePalette.Light
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var deletingCategory by remember { mutableStateOf<Category?>(null) }

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
                    text = stringResource(R.string.manage_categories),
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 34.sp,
                    lineHeight = 37.sp,
                    letterSpacing = (-1).sp,
                    color = palette.ink
                )
                if (categories.isNotEmpty()) {
                    Text(
                        text = pluralStringResource(R.plurals.categories_count, categories.size, categories.size),
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = palette.muted
                    )
                }
            }

            AddCategoryPill(palette, onClick = { showAddDialog = true })

            if (categories.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                ) {
                    Text(
                        text = stringResource(R.string.no_categories),
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        color = palette.ink
                    )
                    Text(
                        text = stringResource(R.string.create_first_category),
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = palette.muted
                    )
                }
            } else {
                val shape = RoundedCornerShape(20.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(palette.surface)
                        .border(1.dp, palette.border, shape)
                ) {
                    categories.forEachIndexed { index, category ->
                        key(category.id) {
                            if (index > 0) HorizontalDivider(color = palette.border, thickness = 1.dp)
                            CategoryRow(
                                category = category,
                                taskCount = taskCountByCategory[category.id] ?: 0,
                                palette = palette,
                                onClick = { editingCategory = category },
                                onDeleteClick = { deletingCategory = category }
                            )
                        }
                    }
                }
            }
        }
    }

    val categoryCreatedText = stringResource(R.string.category_created)
    val categoryUpdatedText = stringResource(R.string.category_updated)
    val categoryDeletedText = stringResource(R.string.category_deleted)

    if (showAddDialog) {
        AddCategoryDialog(
            palette = palette,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, color, icon ->
                onAddCategory(name, color, icon)
                showAddDialog = false
                showToast(categoryCreatedText)
            }
        )
    }

    editingCategory?.let { editing ->
        AddCategoryDialog(
            category = editing,
            palette = palette,
            onDismiss = { editingCategory = null },
            onConfirm = { name, color, icon ->
                onUpdateCategory(editing.copy(nombre = name, color = color, icono = icon))
                editingCategory = null
                showToast(categoryUpdatedText)
            }
        )
    }

    deletingCategory?.let { deleting ->
        DeleteCategoryDialog(
            category = deleting,
            taskCount = taskCountByCategory[deleting.id] ?: 0,
            palette = palette,
            onDismiss = { deletingCategory = null },
            onConfirm = {
                onDeleteCategory(deleting)
                deletingCategory = null
                showToast(categoryDeletedText)
            }
        )
    }
}

@Composable
private fun AddCategoryPill(palette: HomePalette, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(palette.surface)
            .border(1.dp, palette.border, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(palette.brand.copy(alpha = if (palette.isDark) 0.22f else 0.14f))
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = palette.brand, modifier = Modifier.size(18.dp))
        }
        Text(
            text = stringResource(R.string.add_category),
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = palette.placeholder
        )
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    taskCount: Int,
    palette: HomePalette,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val accent = remember(category.color) { category.accentColor() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accent)
        ) {
            Icon(getCategoryIcon(category.icono), contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
            Text(
                text = CategoryUtils.getCategoryName(category),
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = palette.ink
            )
            Text(
                text = pluralStringResource(R.plurals.widget_task_count, taskCount, taskCount),
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = palette.muted
            )
        }
        IconButton(onClick = onDeleteClick) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_category), tint = palette.muted)
        }
    }
}
