package com.example.taksy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.taksy.R
import com.example.taksy.data.Category
import com.example.taksy.ui.theme.HomePalette
import com.example.taksy.ui.theme.PlusJakartaSans
import com.example.taksy.utils.CategoryUtils
import com.example.taksy.utils.getCategoryIcon

/** Colores que se pueden elegir para una categoría (hex guardado en `Category.color`). */
private val categoryColors = listOf(
    "#2196F3", "#4CAF50", "#FF9800", "#F44336", "#9C27B0",
    "#795548", "#607D8B", "#E91E63", "#00BCD4", "#FFC107"
)

/**
 * Diálogo para crear o editar una categoría: nombre, color e icono, con una vista previa
 * del icono tal como se verá en la lista.
 */
@Composable
fun AddCategoryDialog(
    palette: HomePalette,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit,
    category: Category? = null
) {
    var categoryName by remember { mutableStateOf(category?.nombre ?: "") }
    var selectedColor by remember { mutableStateOf(category?.color ?: categoryColors.first()) }
    var selectedIcon by remember { mutableStateOf(category?.icono ?: "work") }
    val accent = remember(selectedColor) { Color(android.graphics.Color.parseColor(selectedColor)) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        HomeDialogSurface(palette) {
            DialogTitle(
                if (category != null) stringResource(R.string.edit_category) else stringResource(R.string.add_category),
                palette
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(accent)
                ) {
                    Icon(getCategoryIcon(selectedIcon), contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
                val fieldShape = RoundedCornerShape(16.dp)
                BasicTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    singleLine = true,
                    cursorBrush = SolidColor(accent),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    textStyle = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = palette.ink),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (categoryName.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.category_name),
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = palette.placeholder
                                )
                            }
                            inner()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(fieldShape)
                        .background(palette.background)
                        .border(1.dp, palette.border, fieldShape)
                        .padding(horizontal = 16.dp)
                        .wrapContentHeight(Alignment.CenterVertically)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DialogSectionLabel(stringResource(R.string.category_color), palette)
                ChoiceGrid(categoryColors) { color ->
                    val selected = color == selectedColor
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(color)))
                            .selectable(selected = selected, role = Role.RadioButton, onClick = { selectedColor = color })
                    ) {
                        if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DialogSectionLabel(stringResource(R.string.category_icon), palette)
                ChoiceGrid(CategoryUtils.selectableIcons) { (name, icon) ->
                    val selected = name == selectedIcon
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) accent else palette.track)
                            .selectable(selected = selected, role = Role.RadioButton, onClick = { selectedIcon = name })
                    ) {
                        Icon(icon, contentDescription = null, tint = if (selected) Color.White else palette.muted, modifier = Modifier.size(22.dp))
                    }
                }
            }

            DialogButtons(
                confirmText = stringResource(R.string.save),
                confirmColor = palette.brand,
                confirmEnabled = categoryName.isNotBlank(),
                palette = palette,
                onDismiss = onDismiss,
                onConfirm = { onConfirm(categoryName.trim(), selectedColor, selectedIcon) }
            )
        }
    }
}

/** Rejilla de 5 columnas con huecos iguales (colores e iconos: 10 opciones, 2 filas). */
@Composable
private fun <T> ChoiceGrid(items: List<T>, cell: @Composable (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(5).forEach { row ->
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                row.forEach { cell(it) }
            }
        }
    }
}

@Composable
private fun DialogSectionLabel(text: String, palette: HomePalette) {
    Text(
        text = text.uppercase(),
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 12.sp,
        letterSpacing = 1.sp,
        color = palette.muted
    )
}
