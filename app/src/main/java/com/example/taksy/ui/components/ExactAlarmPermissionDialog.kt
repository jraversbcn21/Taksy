package com.example.taksy.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.taksy.R
import com.example.taksy.service.AlarmPolicy

/**
 * Explica por qué los recordatorios pueden llegar tarde y lleva a "Alarmas y recordatorios".
 * Al conceder el permiso, ReminderReceiver reprograma las alarmas ya existentes como exactas.
 */
@Composable
fun ExactAlarmPermissionDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.exact_alarm_title)) },
        text = { Text(stringResource(R.string.exact_alarm_message)) },
        confirmButton = {
            TextButton(onClick = {
                context.startActivity(AlarmPolicy.exactAlarmSettingsIntent(context))
                onDismiss()
            }) { Text(stringResource(R.string.exact_alarm_enable)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.exact_alarm_later)) }
        }
    )
}
