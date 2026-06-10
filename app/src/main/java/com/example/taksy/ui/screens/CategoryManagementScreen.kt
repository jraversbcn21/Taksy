package com.example.taksy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.taksy.R
import com.example.taksy.data.Category
import com.example.taksy.ui.components.AddCategoryDialog
import com.example.taksy.ui.components.CategoryItem
import com.example.taksy.ui.components.DeleteCategoryDialog
import com.example.taksy.viewmodel.TaskViewModel

/**
 * Pantalla para gestionar categorías
 * Permite ver, añadir, editar y eliminar categorías
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    categories: List<Category>,
    onBackClick: () -> Unit,
    onAddCategory: (String, String, String) -> Unit,
    onUpdateCategory: (Category) -> Unit,
    onDeleteCategory: (Category) -> Unit,
    showToast: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manage_categories)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_category))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_category))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (categories.isEmpty()) {
                // Mensaje cuando no hay categorías
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.no_categories),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.create_first_category),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { showAddDialog = true }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.add_category))
                        }
                    }
                }
            } else {
                // Lista de categorías
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories, key = { it.id }) { category ->
                        CategoryItem(
                            category = category
                        )
                    }
                }
            }
        }
    }

    // Obtener strings una sola vez
    val categoryCreatedText = stringResource(R.string.category_created)
    val categoryUpdatedText = stringResource(R.string.category_updated)
    val categoryDeletedText = stringResource(R.string.category_deleted)

    // Dialog para añadir categoría
    if (showAddDialog) {
        AddCategoryDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, color, icon ->
                onAddCategory(name, color, icon)
                showAddDialog = false
                showToast(categoryCreatedText)
            }
        )
    }

    // Dialog para editar categoría
    selectedCategory?.takeIf { showEditDialog }?.let { editing ->
        AddCategoryDialog(
            category = editing,
            onDismiss = {
                showEditDialog = false
                selectedCategory = null
            },
            onConfirm = { name, color, icon ->
                onUpdateCategory(editing.copy(nombre = name, color = color, icono = icon))
                showEditDialog = false
                selectedCategory = null
                showToast(categoryUpdatedText)
            }
        )
    }

    // Dialog para eliminar categoría
    selectedCategory?.takeIf { showDeleteDialog }?.let { deleting ->
        DeleteCategoryDialog(
            category = deleting,
            onDismiss = {
                showDeleteDialog = false
                selectedCategory = null
            },
            onConfirm = {
                onDeleteCategory(deleting)
                showDeleteDialog = false
                selectedCategory = null
                showToast(categoryDeletedText)
            }
        )
    }
}
