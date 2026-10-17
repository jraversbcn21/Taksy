package com.example.taksy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taksy.ui.theme.HomePalette
import com.example.taksy.ui.theme.PlusJakartaSans

/** Una opción de [OptionPills]: valor, texto y color cuando está seleccionada. */
data class PillOption<T>(val value: T, val labelRes: Int, val color: Color)

/**
 * Selector de una opción entre varias, como fila de pastillas con una etiqueta encima.
 * Se usa para la prioridad y la repetición de una tarea.
 */
@Composable
fun <T> OptionPills(
    label: String,
    options: List<PillOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    palette: HomePalette,
    modifier: Modifier = Modifier
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        Text(
            text = label.uppercase(),
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
            color = palette.muted
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            options.forEach { option ->
                val isSelected = option.value == selected
                val pill = RoundedCornerShape(10.dp)
                Text(
                    text = stringResource(option.labelRes),
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (isSelected) option.color else palette.muted,
                    modifier = Modifier
                        .clip(pill)
                        .background(
                            if (isSelected) option.color.copy(alpha = if (palette.isDark) 0.2f else 0.12f)
                            else Color.Transparent
                        )
                        .border(1.dp, if (isSelected) option.color else palette.border, pill)
                        .clickable(role = Role.RadioButton) { onSelect(option.value) }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }
    }
}
