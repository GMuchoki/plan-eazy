package com.nesh.planeazy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nesh.planeazy.data.model.Constants
import com.nesh.planeazy.ui.viewmodel.AuthViewModel
import com.nesh.planeazy.ui.viewmodel.TransactionViewModel
import com.nesh.planeazy.util.ExportHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    transactionViewModel: TransactionViewModel,
    authViewModel: AuthViewModel
) {
    val isDarkMode by authViewModel.isDarkMode.collectAsState()
    val currency by authViewModel.currency.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    val user by authViewModel.user.collectAsState()
    val allTransactions by transactionViewModel.allTransactions.collectAsState()
    val isSyncing by transactionViewModel.isSyncing.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // Listen for sync messages from ViewModel
    LaunchedEffect(Unit) {
        transactionViewModel.syncMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            user?.let {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = "Logged in as", style = MaterialTheme.typography.labelSmall)
                            Text(text = it.email ?: "No email", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            SettingsItem(
                icon = Icons.Default.Payments,
                title = "Currency",
                subtitle = currency,
                onClick = { showCurrencyDialog = true }
            )
            
            SettingsItem(
                icon = Icons.Default.DarkMode,
                title = "Dark Mode",
                trailing = {
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { authViewModel.toggleDarkMode(it) }
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Cloud Sync Actions
            Text(
                "Cloud Sync",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            SettingsItem(
                icon = if (isSyncing) Icons.Default.Sync else Icons.Default.CloudUpload,
                title = "Backup to Cloud",
                subtitle = if (isSyncing) "Uploading..." else "Save data to your account",
                onClick = {
                    transactionViewModel.syncToCloud()
                }
            )

            SettingsItem(
                icon = if (isSyncing) Icons.Default.Sync else Icons.Default.CloudDownload,
                title = "Restore from Cloud",
                subtitle = "Recover data from your account",
                onClick = {
                    transactionViewModel.restoreFromCloud()
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsItem(
                icon = Icons.Default.Info,
                title = "Seed Sample Data",
                subtitle = "Fill app with test data",
                onClick = {
                    transactionViewModel.seedSampleData()
                    scope.launch {
                        snackbarHostState.showSnackbar("Sample data added!")
                    }
                }
            )

            SettingsItem(
                icon = Icons.Default.FileUpload,
                title = "Export Data",
                subtitle = "CSV, EXCEL (CSV)",
                onClick = { showExportDialog = true }
            )

            SettingsItem(
                icon = Icons.Default.DeleteForever,
                title = "Reset Data",
                subtitle = "Wipe all local data",
                titleColor = MaterialTheme.colorScheme.error,
                onClick = { showResetDialog = true }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsItem(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = "Logout",
                subtitle = "Clears local cache and signs out",
                titleColor = MaterialTheme.colorScheme.error,
                onClick = {
                    transactionViewModel.resetAllData()
                    authViewModel.signOut()
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                "Version 1.0.0",
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("Export Transactions") },
                text = { Text("Choose a format to export your transaction history.") },
                confirmButton = {
                    Row {
                        TextButton(onClick = {
                            ExportHelper.exportToCSV(context, allTransactions)
                            showExportDialog = false
                        }) {
                            Text("CSV")
                        }
                        TextButton(onClick = {
                            ExportHelper.exportToExcelFriendly(context, allTransactions)
                            showExportDialog = false
                        }) {
                            Text("EXCEL")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showCurrencyDialog) {
            AlertDialog(
                onDismissRequest = { showCurrencyDialog = false },
                title = { Text("Select Currency") },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Constants.CURRENCIES.forEach { (code, name) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .height(48.dp)
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currency == code,
                                    onClick = {
                                        authViewModel.setCurrency(code)
                                        showCurrencyDialog = false
                                    }
                                )
                                Text(
                                    text = "$code - $name",
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCurrencyDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Reset All Data?") },
                text = { Text("This will permanently delete all local transactions and budgets.") },
                confirmButton = {
                    TextButton(onClick = {
                        transactionViewModel.resetAllData()
                        showResetDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar("All data cleared")
                        }
                    }) {
                        Text("Reset Everything", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    titleColor: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon, 
                contentDescription = null, 
                tint = if (titleColor != Color.Unspecified) titleColor else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = titleColor, fontWeight = FontWeight.Medium)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            trailing?.invoke()
        }
    }
}
