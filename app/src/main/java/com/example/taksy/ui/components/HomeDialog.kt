package com.example.taksy.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.taksy.R
import com.example.taksy.ui.theme.HomePalette
import com.example.taksy.ui.theme.PlusJakartaSans

/*
 * Piezas comunes de los diálogos con el lenguaje visual de la pantalla de inicio
 * (HomePalette + Plus Jakarta Sans): tarjeta, título, botones y selectores de fecha/hora.
 */

/** Tarjeta de diálogo con el lenguaje visual de la pantalla de inicio. */
@Composable
fun HomeDialogSurface(palette: HomePalette, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .widthIn(max = 420.dp)
            .clip(shape)
            .background(palette.surface)
            .border(1.dp, palette.border, shape)
            .padding(22.dp),
        content = content
    )
}

/** Diálogo simple: título, mensaje y botones (Cancelar + acción). */
@Composable
fun HomeMessageDialog(
    title: String,
    message: String,
    confirmText: String,
    confirmColor: Color,
    palette: HomePalette,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String? = null
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        HomeDialogSurface(palette) {
            DialogTitle(title, palette)
            Text(
                text = message,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                color = palette.muted
            )
            DialogButtons(
                confirmText = confirmText,
                confirmColor = confirmColor,
                palette = palette,
                onDismiss = onDismiss,
                onConfirm = onConfirm,
                dismissText = dismissText
            )
        }
    }
}

@Composable
fun DialogTitle(text: String, palette: HomePalette) {
    Text(
        text = text,
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp,
        letterSpacing = (-0.5).sp,
        color = palette.ink
    )
}

/**
 * Botón secundario (texto, "Cancelar" por defecto) + acción principal (píldora del color indicado).
 * [leading] queda a la izquierda, p. ej. para "Eliminar".
 */
@Composable
fun DialogButtons(
    confirmText: String,
    confirmColor: Color,
    palette: HomePalette,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    dismissText: String? = null,
    leading: (@Composable () -> Unit)? = null
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.weight(1f))
        }
        DialogTextButton(dismissText ?: stringResource(R.string.cancel), palette.muted, onDismiss)
        Text(
            text = confirmText,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = if (confirmEnabled) Color.White else palette.doneText,
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(if (confirmEnabled) confirmColor else palette.track)
                .clickable(enabled = confirmEnabled, role = Role.Button, onClick = onConfirm)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        )
    }
}

/** Botón de texto de los diálogos (sin fondo). */
@Composable
fun DialogTextButton(text: String, color: Color, onClick: () -> Unit) {
    Text(
        text = text,
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    )
}

/** Colores del TimePicker de Material3 con la paleta de inicio y el acento de la pantalla. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun homeTimePickerColors(palette: HomePalette, accent: Color): TimePickerColors = TimePickerDefaults.colors(
    clockDialColor = palette.track,
    clockDialSelectedContentColor = Color.White,
    clockDialUnselectedContentColor = palette.ink,
    selectorColor = accent,
    containerColor = palette.surface,
    periodSelectorBorderColor = palette.border,
    periodSelectorSelectedContainerColor = accent.copy(alpha = if (palette.isDark) 0.3f else 0.16f),
    periodSelectorUnselectedContainerColor = Color.Transparent,
    periodSelectorSelectedContentColor = palette.ink,
    periodSelectorUnselectedContentColor = palette.muted,
    timeSelectorSelectedContainerColor = accent.copy(alpha = if (palette.isDark) 0.3f else 0.16f),
    timeSelectorUnselectedContainerColor = palette.track,
    timeSelectorSelectedContentColor = palette.ink,
    timeSelectorUnselectedContentColor = palette.ink
)

/**
 * Selector de hora en un diálogo de inicio. El botón secundario cierra por defecto; con
 * [dismissText] y [onDismissButton] puede hacer otra cosa (p. ej. "Sin hora").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTimePickerDialog(
    palette: HomePalette,
    accent: Color,
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
    dismissText: String? = null,
    onDismissButton: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val state = rememberTimePickerState(initialHour, initialMinute, DateFormat.is24HourFormat(context))
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        HomeDialogSurface(palette) {
            TimePicker(
                state = state,
                colors = homeTimePickerColors(palette, accent),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            DialogButtons(
                confirmText = stringResource(R.string.ok),
                confirmColor = accent,
                palette = palette,
                onDismiss = onDismissButton ?: onDismiss,
                onConfirm = { onConfirm(state.hour, state.minute) },
                dismissText = dismissText
            )
        }
    }
}

/**
 * Selector de día (Material3 DatePicker) con la paleta de inicio. Trabaja en medianoche UTC,
 * como el DatePicker: usar DateUtils.toUtcDay / combineUtcDay para convertir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDatePickerDialog(
    palette: HomePalette,
    accent: Color,
    initialUtcMillis: Long,
    onConfirm: (utcMillis: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialUtcMillis)
    val colors = DatePickerDefaults.colors(
        containerColor = palette.surface,
        titleContentColor = palette.muted,
        headlineContentColor = palette.ink,
        weekdayContentColor = palette.muted,
        subheadContentColor = palette.muted,
        navigationContentColor = palette.ink,
        yearContentColor = palette.ink,
        currentYearContentColor = accent,
        selectedYearContentColor = Color.White,
        selectedYearContainerColor = accent,
        dayContentColor = palette.ink,
        selectedDayContentColor = Color.White,
        selectedDayContainerColor = accent,
        todayContentColor = accent,
        todayDateBorderColor = accent,
        dividerColor = palette.border
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        colors = colors,
        confirmButton = {
            val selected = state.selectedDateMillis
            DialogTextButton(stringResource(R.string.ok), if (selected != null) accent else palette.doneText) {
                selected?.let(onConfirm)
            }
        },
        dismissButton = { DialogTextButton(stringResource(R.string.cancel), palette.muted, onDismiss) }
    ) {
        DatePicker(state = state, colors = colors, showModeToggle = false)
    }
}
