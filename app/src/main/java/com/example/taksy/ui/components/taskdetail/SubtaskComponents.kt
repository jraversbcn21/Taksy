package com.example.taksy.ui.components.taskdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taksy.R
import com.example.taksy.data.Subtask
import com.example.taksy.data.TaskEstado
import com.example.taksy.ui.theme.HomePalette
import com.example.taksy.ui.theme.PlusJakartaSans

/** Fila de subtarea dentro de la tarjeta de subtareas. Una subtarea completada no se puede reabrir. */
@Composable
fun SubtaskItem(
    subtask: Subtask,
    palette: HomePalette,
    accent: Color,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompleted = subtask.estado == TaskEstado.COMPLETADA
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled && !isCompleted, role = Role.Checkbox, onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (isCompleted) accent else Color.Transparent)
                .then(if (isCompleted) Modifier else Modifier.border(2.dp, accent, CircleShape))
        ) {
            if (isCompleted) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = subtask.titulo,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = if (isCompleted) palette.doneText else palette.ink,
            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
        )
    }
}

/** Última fila de la tarjeta de subtareas: "+ Añadir subtarea", que se convierte en un campo de texto. */
@Composable
fun AddSubtaskRow(
    isEditing: Boolean,
    subtaskTitle: String,
    onSubtaskTitleChange: (String) -> Unit,
    onStartEditing: () -> Unit,
    onAddSubtask: (String) -> Unit,
    onCancel: () -> Unit,
    focusRequester: FocusRequester,
    palette: HomePalette,
    accent: Color,
    enabled: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled && !isEditing, role = Role.Button, onClick = onStartEditing)
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
            .heightIn(min = 44.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(accent.copy(alpha = if (palette.isDark) 0.22f else 0.14f))
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(14.dp))
        if (isEditing) {
            BasicTextField(
                value = subtaskTitle,
                onValueChange = onSubtaskTitleChange,
                singleLine = true,
                cursorBrush = SolidColor(accent),
                textStyle = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = palette.ink),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (subtaskTitle.isNotBlank()) onAddSubtask(subtaskTitle.trim()) }),
                decorationBox = { inner ->
                    if (subtaskTitle.isEmpty()) {
                        Text(
                            text = stringResource(R.string.add_subtask),
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = palette.placeholder
                        )
                    }
                    inner()
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
            )
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel), tint = palette.muted)
            }
        } else {
            Text(
                text = stringResource(R.string.add_subtask),
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (enabled) palette.placeholder else palette.doneText,
                modifier = Modifier.padding(vertical = 10.dp)
            )
        }
    }
}
