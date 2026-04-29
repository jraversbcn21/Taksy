package com.example.taksy.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import android.util.Log
import com.example.taksy.R
import com.example.taksy.data.Category
import com.example.taksy.utils.getCategoryIcon

/**
 * Dialog para añadir o editar una categoría
 */
@Composable
fun AddCategoryDialog(
    category: Category? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit,
    activity: androidx.activity.ComponentActivity? = null
) {
    val view = LocalView.current
    val context = view.context
    var categoryName by remember { mutableStateOf(category?.nombre ?: "") }
    var selectedColor by remember { mutableStateOf(category?.color ?: "#2196F3") }
    var selectedIcon by remember { mutableStateOf(category?.icono ?: "work") }

    // Colores predefinidos
    val colors = listOf(
        "#2196F3", // Azul
        "#4CAF50", // Verde
        "#FF9800", // Naranja
        "#F44336", // Rojo
        "#9C27B0", // Morado
        "#795548", // Marrón
        "#607D8B", // Azul gris
        "#E91E63", // Rosa
        "#00BCD4", // Cian
        "#FFC107"  // Amarillo
    )

    // Iconos predefinidos
    val icons = listOf(
        "work" to Icons.Default.Build,
        "person" to Icons.Default.Person,
        "shopping_cart" to Icons.Default.ShoppingCart,
        "favorite" to Icons.Default.Favorite,
        "school" to Icons.Default.Settings,
        "home" to Icons.Default.Home,
        "star" to Icons.Default.Star,
        "schedule" to Icons.Default.DateRange,
        "business" to Icons.Default.Info,
        "sports" to Icons.Default.Notifications
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = if (category != null) stringResource(R.string.edit_category) else stringResource(R.string.add_category),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                // Content
                // Campo de nombre
                Column {
                    Text(
                        text = stringResource(R.string.category_name),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Usar BasicTextField con control total del color
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline
                        ),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        BasicTextField(
                            value = categoryName,
                            onValueChange = { categoryName = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = MaterialTheme.typography.bodyLarge.fontSize
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                if (categoryName.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.category_name_hint),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }

                // Selector de color
                Text(
                    text = stringResource(R.string.category_color),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(colors) { color ->
                        val isSelected = selectedColor == color
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .padding(2.dp)
                        ) {
                            Card(
                                modifier = Modifier.fillMaxSize(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(android.graphics.Color.parseColor(color))
                                ),
                                shape = CircleShape,
                                onClick = { selectedColor = color },
                                border = if (isSelected) {
                                    CardDefaults.outlinedCardBorder().copy(
                                        width = 3.dp
                                    )
                                } else null
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Selector de icono
                Text(
                    text = stringResource(R.string.category_icon),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(icons) { (iconName, icon) ->
                        val isSelected = selectedIcon == iconName
                        Card(
                            modifier = Modifier.size(48.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                            onClick = { selectedIcon = iconName },
                            border = if (isSelected) {
                                CardDefaults.outlinedCardBorder().copy(
                                    width = 2.dp
                                )
                            } else null
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
                
                // Botones
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (categoryName.isNotBlank()) {
                                onConfirm(categoryName.trim(), selectedColor, selectedIcon)
                            }
                        },
                        enabled = categoryName.isNotBlank()
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }
}
