package com.example.taksy.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taksy.R
import com.example.taksy.ui.components.HomeMessageDialog
import com.example.taksy.ui.theme.DueOverdueRed
import com.example.taksy.ui.theme.HomePalette
import com.example.taksy.ui.theme.PlusJakartaSans
import com.example.taksy.utils.BackupManager
import com.example.taksy.viewmodel.BackupState
import com.example.taksy.viewmodel.BackupViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exportar / importar todos los datos a un fichero JSON elegido con SAF.
 * Importar reemplaza todos los datos, así que pide confirmación antes.
 * Mismo lenguaje visual que la pantalla de inicio (HomePalette + Plus Jakarta Sans).
 */
@Composable
fun BackupScreen(
    backupViewModel: BackupViewModel,
    onBackClick: () -> Unit,
    isDarkMode: Boolean = false
) {
    val palette = if (isDarkMode) HomePalette.Dark else HomePalette.Light
    val danger = if (palette.isDark) lerp(DueOverdueRed, Color.White, 0.25f) else DueOverdueRed
    val context = LocalContext.current
    val backupState by backupViewModel.backupState.collectAsState()
    val isLoading = backupState is BackupState.Loading
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? -> uri?.let { backupViewModel.exportBackup(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> pendingImportUri = uri }

    LaunchedEffect(backupState) {
        val message = when (val state = backupState) {
            is BackupState.ExportSuccess -> context.getString(R.string.backup_export_success)
            is BackupState.ImportSuccess -> context.getString(
                R.string.backup_import_success,
                state.categories, state.tasks, state.subtasks, state.reminders
            )
            is BackupState.ImportError -> {
                val detail = state.detail ?: ""
                when (state.errorType) {
                    BackupManager.ImportErrorType.INVALID_JSON -> context.getString(R.string.backup_error_invalid_json)
                    BackupManager.ImportErrorType.MISSING_SECTION -> context.getString(R.string.backup_error_missing_section, detail)
                    BackupManager.ImportErrorType.INVALID_CATEGORY -> context.getString(R.string.backup_error_invalid_category, detail)
                    BackupManager.ImportErrorType.INVALID_TASK -> context.getString(R.string.backup_error_invalid_task, detail)
                    BackupManager.ImportErrorType.INVALID_SUBTASK -> context.getString(R.string.backup_error_invalid_subtask, detail)
                    BackupManager.ImportErrorType.INVALID_REMINDER -> context.getString(R.string.backup_error_invalid_reminder, detail)
                    BackupManager.ImportErrorType.INVALID_DATE -> context.getString(R.string.backup_error_invalid_date, detail)
                }
            }
            is BackupState.Error -> context.getString(R.string.backup_error, state.message)
            else -> null
        }
        if (message != null) {
            // Reset después: cambiar el estado relanza este efecto y cancelaría el snackbar
            snackbarHostState.showSnackbar(message)
            backupViewModel.resetState()
        }
    }

    Scaffold(
        containerColor = palette.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 32.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                IconButton(onClick = onBackClick, modifier = Modifier.offset(x = (-12).dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = palette.ink
                    )
                }
                Text(
                    text = stringResource(R.string.backup_title),
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 34.sp,
                    lineHeight = 37.sp,
                    letterSpacing = (-1).sp,
                    color = palette.ink
                )
            }

            BackupCard(palette) {
                ActionRow(
                    icon = Icons.Default.Share,
                    tileColor = Color(0xFF00897B),
                    title = stringResource(R.string.backup_export),
                    description = stringResource(R.string.backup_export_description),
                    enabled = !isLoading,
                    palette = palette,
                    onClick = {
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                        exportLauncher.launch("taksy_backup_$timestamp.json")
                    }
                )
            }

            BackupCard(palette) {
                ActionRow(
                    icon = Icons.Default.Refresh,
                    tileColor = Color(0xFF1565C0),
                    title = stringResource(R.string.backup_import),
                    description = stringResource(R.string.backup_import_description),
                    enabled = !isLoading,
                    palette = palette,
                    onClick = { importLauncher.launch(arrayOf("application/json")) }
                )
                HorizontalDivider(color = palette.border, thickness = 1.dp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(danger.copy(alpha = if (palette.isDark) 0.12f else 0.06f))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = danger, modifier = Modifier.size(18.dp))
                    Text(
                        text = stringResource(R.string.backup_import_warning),
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = danger
                    )
                }
            }

            if (isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator(color = palette.brand, strokeWidth = 3.dp, modifier = Modifier.size(22.dp))
                    Text(
                        text = stringResource(R.string.backup_processing),
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = palette.muted
                    )
                }
            }
        }
    }

    pendingImportUri?.let { uri ->
        HomeMessageDialog(
            title = stringResource(R.string.backup_confirm_import_title),
            message = stringResource(R.string.backup_confirm_import_message),
            confirmText = stringResource(R.string.backup_import),
            confirmColor = danger,
            palette = palette,
            onConfirm = {
                pendingImportUri = null
                backupViewModel.importBackup(uri)
            },
            onDismiss = { pendingImportUri = null }
        )
    }
}

@Composable
private fun BackupCard(palette: HomePalette, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.surface)
            .border(1.dp, palette.border, shape),
        content = content
    )
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    tileColor: Color,
    title: String,
    description: String,
    enabled: Boolean,
    palette: HomePalette,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(start = 16.dp, end = 12.dp, top = 16.dp, bottom = 16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(tileColor)
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = palette.ink
            )
            Text(
                text = description,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = palette.muted
            )
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = palette.muted)
    }
}
