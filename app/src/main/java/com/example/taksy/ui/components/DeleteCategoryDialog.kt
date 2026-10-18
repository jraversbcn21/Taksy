package com.example.taksy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.taksy.R
import com.example.taksy.data.Category
import com.example.taksy.ui.theme.DueOverdueRed
import com.example.taksy.ui.theme.HomePalette
import com.example.taksy.ui.theme.PlusJakartaSans
import com.example.taksy.ui.theme.accentColor
import com.example.taksy.utils.CategoryUtils
import com.example.taksy.utils.getCategoryIcon

/** Confirmación para eliminar una categoría; avisa de cuántas tareas se borran con ella. */
@Composable
fun DeleteCategoryDialog(
    category: Category,
    taskCount: Int,
    palette: HomePalette,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val accent = remember(category.color) { category.accentColor() }
    val danger = if (palette.isDark) lerp(DueOverdueRed, Color.White, 0.25f) else DueOverdueRed

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        HomeDialogSurface(palette) {
            DialogTitle(stringResource(R.string.confirm_delete_category), palette)

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent)
                ) {
                    Icon(getCategoryIcon(category.icono), contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Text(
                    text = CategoryUtils.getCategoryName(category),
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = palette.ink
                )
            }

            if (taskCount > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(danger.copy(alpha = if (palette.isDark) 0.16f else 0.08f))
                        .padding(14.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = danger, modifier = Modifier.size(20.dp))
                    Text(
                        text = pluralStringResource(R.plurals.delete_category_tasks_warning, taskCount, taskCount),
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = danger
                    )
                }
            }

            DialogButtons(
                confirmText = stringResource(R.string.delete),
                confirmColor = danger,
                palette = palette,
                onDismiss = onDismiss,
                onConfirm = onConfirm
            )
        }
    }
}
