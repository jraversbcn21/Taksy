package com.example.taksy.ui.components.taskdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.taksy.R
import com.example.taksy.data.Task
import com.example.taksy.data.TaskRecurrencia
import com.example.taksy.ui.theme.RecurrencePurple

@Composable
fun RecurrenceSection(
    task: Task,
    onRecurrenceChange: (TaskRecurrencia) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = RecurrencePurple,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.recurrence),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        val recurrences = listOf(
            TaskRecurrencia.NINGUNA to R.string.recurrence_none,
            TaskRecurrencia.DIARIA to R.string.recurrence_daily,
            TaskRecurrencia.SEMANAL to R.string.recurrence_weekly,
            TaskRecurrencia.MENSUAL to R.string.recurrence_monthly
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            recurrences.forEach { (recurrence, labelRes) ->
                val isSelected = task.recurrencia == recurrence
                val chipColor = if (recurrence == TaskRecurrencia.NINGUNA)
                    MaterialTheme.colorScheme.outline else RecurrencePurple
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (task.recurrencia != recurrence) onRecurrenceChange(recurrence)
                    },
                    label = {
                        Text(stringResource(labelRes), style = MaterialTheme.typography.labelSmall)
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
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    }
}
