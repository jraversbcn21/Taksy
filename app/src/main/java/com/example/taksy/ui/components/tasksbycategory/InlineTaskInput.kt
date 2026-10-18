package com.example.taksy.ui.components.tasksbycategory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taksy.R
import com.example.taksy.data.TaskPrioridad
import com.example.taksy.data.TaskRecurrencia
import com.example.taksy.ui.components.OptionPills
import com.example.taksy.ui.components.PillOption
import com.example.taksy.ui.theme.HomePalette
import com.example.taksy.ui.theme.PlusJakartaSans
import com.example.taksy.ui.theme.PriorityHighRed
import com.example.taksy.ui.theme.PriorityLowGreen
import com.example.taksy.ui.theme.PriorityMediumOrange

/**
 * Tarjeta para crear una tarea en la lista: título, prioridad y repetición.
 * El botón "Añadir tarea" guarda y cierra (keepOpen = false); el Enter del teclado guarda
 * y la deja abierta para encadenar otra (keepOpen = true).
 */
@Composable
fun InlineTaskInput(
    taskTitle: String,
    onTaskTitleChange: (String) -> Unit,
    selectedPriority: TaskPrioridad,
    onPriorityChange: (TaskPrioridad) -> Unit,
    selectedRecurrence: TaskRecurrencia,
    onRecurrenceChange: (TaskRecurrencia) -> Unit,
    onAddTask: (title: String, keepOpen: Boolean) -> Unit,
    onCancel: () -> Unit,
    focusRequester: FocusRequester,
    palette: HomePalette,
    accent: Color,
    modifier: Modifier = Modifier
) {
    var hasSubmitted by remember { mutableStateOf(false) }
    LaunchedEffect(taskTitle) { if (taskTitle.isEmpty()) hasSubmitted = false }

    fun submit(keepOpen: Boolean) {
        if (taskTitle.isNotBlank() && !hasSubmitted) {
            hasSubmitted = true
            onAddTask(taskTitle.trim(), keepOpen)
        }
    }

    val shape = RoundedCornerShape(20.dp)
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.surface)
            .border(1.5.dp, accent.copy(alpha = 0.45f), shape)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(24.dp)
                    .border(2.dp, accent.copy(alpha = 0.5f), CircleShape)
            )
            BasicTextField(
                value = taskTitle,
                onValueChange = onTaskTitleChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                singleLine = true,
                cursorBrush = SolidColor(accent),
                textStyle = TextStyle(
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = palette.ink
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { submit(keepOpen = true) }),
                decorationBox = { innerTextField ->
                    if (taskTitle.isEmpty()) {
                        Text(
                            text = stringResource(R.string.task_title_hint),
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = palette.placeholder
                        )
                    }
                    innerTextField()
                }
            )
        }

        OptionPills(
            label = stringResource(R.string.task_priority),
            options = priorityOptions(palette),
            selected = selectedPriority,
            onSelect = onPriorityChange,
            palette = palette
        )

        OptionPills(
            label = stringResource(R.string.recurrence),
            options = recurrenceOptions(palette, accent),
            selected = selectedRecurrence,
            onSelect = onRecurrenceChange,
            palette = palette
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.cancel),
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = palette.muted,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button, onClick = onCancel)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            )
            val canAdd = taskTitle.isNotBlank()
            Text(
                text = stringResource(R.string.add_task),
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (canAdd) accent else accent.copy(alpha = 0.35f))
                    .clickable(enabled = canAdd, role = Role.Button) { submit(keepOpen = false) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}

fun priorityOptions(palette: HomePalette) = listOf(
    PillOption(TaskPrioridad.NINGUNA, R.string.priority_none, palette.muted),
    PillOption(TaskPrioridad.BAJA, R.string.priority_low, PriorityLowGreen),
    PillOption(TaskPrioridad.MEDIA, R.string.priority_medium, PriorityMediumOrange),
    PillOption(TaskPrioridad.ALTA, R.string.priority_high, PriorityHighRed)
)

fun recurrenceOptions(palette: HomePalette, accent: Color) = listOf(
    PillOption(TaskRecurrencia.NINGUNA, R.string.recurrence_none, palette.muted),
    PillOption(TaskRecurrencia.DIARIA, R.string.recurrence_daily, accent),
    PillOption(TaskRecurrencia.SEMANAL, R.string.recurrence_weekly, accent),
    PillOption(TaskRecurrencia.MENSUAL, R.string.recurrence_monthly, accent)
)
