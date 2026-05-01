package com.example.taksy.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.taksy.R
import com.example.taksy.utils.BackupManager
import com.example.taksy.viewmodel.BackupState
import com.example.taksy.viewmodel.BackupViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    backupViewModel: BackupViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val backupState by backupViewModel.backupState.collectAsState()
    var showImportConfirmDialog by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Launcher to create a file for export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            backupViewModel.generateBackupJson { json ->
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(json.toByteArray(Charsets.UTF_8))
                }
            }
        }
    }

    // Launcher to pick a file for import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingImportUri = uri
            showImportConfirmDialog = true
        }
    }

    // Show snackbar on state changes
    LaunchedEffect(backupState) {
        when (val state = backupState) {
            is BackupState.ExportSuccess -> {
                snackbarHostState.showSnackbar(context.getString(R.string.backup_export_success))
                backupViewModel.resetState()
            }
            is BackupState.ImportSuccess -> {
                snackbarHostState.showSnackbar(
                    context.getString(
                        R.string.backup_import_success,
                        state.categories, state.tasks, state.subtasks, state.reminders
                    )
                )
                backupViewModel.resetState()
            }
            is BackupState.ImportError -> {
                val msg = when (state.errorType) {
                    BackupManager.ImportErrorType.INVALID_JSON ->
                        context.getString(R.string.backup_error_invalid_json)
                    BackupManager.ImportErrorType.MISSING_SECTION ->
                        context.getString(R.string.backup_error_missing_section, state.detail ?: "")
                    BackupManager.ImportErrorType.INVALID_CATEGORY ->
                        context.getString(R.string.backup_error_invalid_category, state.detail ?: "")
                    BackupManager.ImportErrorType.INVALID_TASK ->
                        context.getString(R.string.backup_error_invalid_task, state.detail ?: "")
                    BackupManager.ImportErrorType.INVALID_SUBTASK ->
                        context.getString(R.string.backup_error_invalid_subtask, state.detail ?: "")
                    BackupManager.ImportErrorType.INVALID_REMINDER ->
                        context.getString(R.string.backup_error_invalid_reminder, state.detail ?: "")
                    BackupManager.ImportErrorType.INVALID_DATE ->
                        context.getString(R.string.backup_error_invalid_date, state.detail ?: "")
                }
                snackbarHostState.showSnackbar(msg)
                backupViewModel.resetState()
            }
            is BackupState.Error -> {
                snackbarHostState.showSnackbar(
                    context.getString(R.string.backup_error, state.message)
                )
                backupViewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.backup_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.close)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Export card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = stringResource(R.string.backup_export),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.backup_export_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                            exportLauncher.launch("taksy_backup_$timestamp.json")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = backupState !is BackupState.Loading
                    ) {
                        Text(stringResource(R.string.backup_export))
                    }
                }
            }

            // Import card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = stringResource(R.string.backup_import),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.backup_import_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.backup_import_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            importLauncher.launch(arrayOf("application/json"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = backupState !is BackupState.Loading
                    ) {
                        Text(stringResource(R.string.backup_import))
                    }
                }
            }

            // Loading indicator
            if (backupState is BackupState.Loading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.backup_processing),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Import confirmation dialog
    if (showImportConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showImportConfirmDialog = false
                pendingImportUri = null
            },
            title = { Text(stringResource(R.string.backup_confirm_import_title)) },
            text = { Text(stringResource(R.string.backup_confirm_import_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showImportConfirmDialog = false
                    pendingImportUri?.let { uri ->
                        val json = context.contentResolver.openInputStream(uri)?.use {
                            it.bufferedReader().readText()
                        }
                        if (json != null) {
                            backupViewModel.importBackupJson(json)
                        }
                    }
                    pendingImportUri = null
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportConfirmDialog = false
                    pendingImportUri = null
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
