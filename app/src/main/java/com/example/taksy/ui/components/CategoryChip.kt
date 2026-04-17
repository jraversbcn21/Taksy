package com.example.taksy.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.taksy.data.Category
import com.example.taksy.utils.CategoryUtils

/**
 * Componente que muestra una categoría como chip
 */
@Composable
fun CategoryChip(
    category: Category,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color(android.graphics.Color.parseColor(category.color)).copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = Color(android.graphics.Color.parseColor(category.color))
        )
    ) {
        Text(
            text = CategoryUtils.getCategoryName(category.icono),
            style = MaterialTheme.typography.labelSmall,
            color = Color(android.graphics.Color.parseColor(category.color)),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
