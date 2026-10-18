package com.example.taksy.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.taksy.R
import com.example.taksy.service.AlarmPolicy
import com.example.taksy.ui.theme.HomePalette

/**
 * Explica por qué los recordatorios pueden llegar tarde y lleva a "Alarmas y recordatorios".
 * Al conceder el permiso, ReminderReceiver reprograma las alarmas ya existentes como exactas.
 */
@Composable
fun ExactAlarmPermissionDialog(palette: HomePalette, onDismiss: () -> Unit) {
    val context = LocalContext.current
    HomeMessageDialog(
        title = stringResource(R.string.exact_alarm_title),
        message = stringResource(R.string.exact_alarm_message),
        confirmText = stringResource(R.string.exact_alarm_enable),
        confirmColor = palette.brand,
        palette = palette,
        onConfirm = {
            context.startActivity(AlarmPolicy.exactAlarmSettingsIntent(context))
            onDismiss()
        },
        onDismiss = onDismiss,
        dismissText = stringResource(R.string.exact_alarm_later)
    )
}
