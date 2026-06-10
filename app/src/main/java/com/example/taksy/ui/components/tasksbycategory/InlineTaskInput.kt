package com.example.taksy.ui.components.tasksbycategory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.taksy.R
import com.example.taksy.data.TaskPrioridad
import com.example.taksy.data.TaskRecurrencia
import com.example.taksy.ui.theme.PriorityHighRed
import com.example.taksy.ui.theme.PriorityLowGreen
import com.example.taksy.ui.theme.PriorityMediumOrange
import com.example.taksy.ui.theme.RecurrencePurple

@Composable
fun InlineTaskInput(
    taskTitle: String,
    onTaskTitleChange: (String) -> Unit,
    selectedPriority: TaskPrioridad,
    onPriorityChange: (TaskPrioridad) -> Unit,
    selectedRecurrence: TaskRecurrencia,
    onRecurrenceChange: (TaskRecurrencia) -> Unit,
    onAddTask: (String) -> Unit,
    onCancel: () -> Unit,
    focusRequester: FocusRequester
) {
    var hasSubmitted by remember { mutableStateOf(false) }
    LaunchedEffect(taskTitle) { if (taskTitle.isEmpty()) hasSubmitted = false }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = false,
                onClick = { },
                enabled = false,
                colors = RadioButtonDefaults.colors(
                    unselectedColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            Spacer(modifier = Modifier.width(12.dp))

            BasicTextField(
                value = taskTitle,
                onValueChange = onTaskTitleChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (taskTitle.isNotBlank() && !hasSubmitted) {
                            hasSubmitted = true
                            onAddTask(taskTitle.trim())
                        }
                    }
                ),
                decorationBox = { innerTextField ->
                    if (taskTitle.isEmpty()) {
                        Text(
                            text = stringResource(R.string.add_task),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    innerTextField()
                }
            )

            if (taskTitle.isNotBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
            }
        }

        PrioritySelector(
            selectedPriority = selectedPriority,
            onPriorityChange = onPriorityChange,
            modifier = Modifier.padding(start = 48.dp, top = 4.dp)
        )

        RecurrenceSelector(
            selectedRecurrence = selectedRecurrence,
            onRecurrenceChange = onRecurrenceChange,
            modifier = Modifier.padding(start = 48.dp, top = 4.dp)
        )
    }
}

@Composable
private fun PrioritySelector(
    selectedPriority: TaskPrioridad,
    onPriorityChange: (TaskPrioridad) -> Unit,
    modifier: Modifier = Modifier
) {
    val priorities = listOf(
        TaskPrioridad.NINGUNA to (R.string.priority_none to Color.Gray),
        TaskPrioridad.BAJA to (R.string.priority_low to PriorityLowGreen),
        TaskPrioridad.MEDIA to (R.string.priority_medium to PriorityMediumOrange),
        TaskPrioridad.ALTA to (R.string.priority_high to PriorityHighRed)
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        priorities.forEach { (priority, pair) ->
            val (labelRes, accentColor) = pair
            val isSelected = selectedPriority == priority

            FilterChip(
                selected = isSelected,
                onClick = { onPriorityChange(priority) },
                label = {
                    Text(text = stringResource(labelRes), style = MaterialTheme.typography.labelSmall)
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accentColor.copy(alpha = 0.15f),
                    selectedLabelColor = accentColor
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    selectedBorderColor = accentColor
                ),
                modifier = Modifier.height(28.dp)
            )
        }
    }
}

@Composable
private fun RecurrenceSelector(
    selectedRecurrence: TaskRecurrencia,
    onRecurrenceChange: (TaskRecurrencia) -> Unit,
    modifier: Modifier = Modifier
) {
    val recurrenceColor = RecurrencePurple
    val recurrences = listOf(
        TaskRecurrencia.NINGUNA to R.string.recurrence_none,
        TaskRecurrencia.DIARIA to R.string.recurrence_daily,
        TaskRecurrencia.SEMANAL to R.string.recurrence_weekly,
        TaskRecurrencia.MENSUAL to R.string.recurrence_monthly
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        recurrences.forEach { (recurrence, labelRes) ->
            val isSelected = selectedRecurrence == recurrence
            val chipColor = if (recurrence == TaskRecurrencia.NINGUNA)
                MaterialTheme.colorScheme.outline else recurrenceColor

            FilterChip(
                selected = isSelected,
                onClick = { onRecurrenceChange(recurrence) },
                label = {
                    Text(text = stringResource(labelRes), style = MaterialTheme.typography.labelSmall)
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = chipColor.copy(alpha = 0.15f),
                    selectedLabelColor = chipColor
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    selectedBorderColor = chipColor
                ),
                modifier = Modifier.height(28.dp)
            )
        }
    }
}
