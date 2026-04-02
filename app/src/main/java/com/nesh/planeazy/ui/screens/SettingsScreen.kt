package com.nesh.planeazy.ui.screens

import androidx.biometric.BiometricManager
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
    val useBiometrics by authViewModel.useBiometrics.collectAsState()
    
    var showResetDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    
    val user by authViewModel.user.collectAsState()
    val allTransactions by transactionViewModel.allTransactions.collectAsState()
    val allDebts by transactionViewModel.allDebts.collectAsState()
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

            // General Section
            SettingsSectionHeader("General")
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

            // Security Section
            SettingsSectionHeader("Security")
            SettingsItem(
                icon = Icons.Default.Fingerprint,
                title = "Biometric Lock",
                subtitle = "Require fingerprint to open app",
                trailing = {
                    Switch(
                        checked = useBiometrics,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                val biometricManager = BiometricManager.from(context)
                                if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
                                    authViewModel.setUseBiometrics(true)
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Biometrics not available on this device")
                                    }
                                }
                            } else {
                                authViewModel.setUseBiometrics(false)
                            }
                        }
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Cloud Sync Section
            SettingsSectionHeader("Cloud Sync")
            SettingsItem(
                icon = if (isSyncing) Icons.Default.Sync else Icons.Default.CloudSync,
                title = "Force Full Re-Sync",
                subtitle = if (isSyncing) "Syncing..." else "Ensure all local data is in the cloud",
                onClick = {
                    transactionViewModel.syncToCloud()
                }
            )

            SettingsItem(
                icon = if (isSyncing) Icons.Default.Sync else Icons.Default.CloudDownload,
                title = "Restore from Cloud",
                subtitle = "Recover your data on this device",
                onClick = {
                    transactionViewModel.restoreFromCloud()
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Data Section
            SettingsSectionHeader("Data & Backup")
            SettingsItem(
                icon = Icons.Default.FileUpload,
                title = "Export Data",
                subtitle = "Save as CSV, Excel or PDF",
                onClick = { showExportDialog = true }
            )

            SettingsItem(
                icon = Icons.Default.Info,
                title = "Seed Sample Data",
                subtitle = "Fill app with test transactions",
                onClick = {
                    transactionViewModel.seedSampleData(
                        onFailure = { error ->
                            scope.launch {
                                snackbarHostState.showSnackbar(error)
                            }
                        }
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Danger Zone
            SettingsSectionHeader("Danger Zone", color = MaterialTheme.colorScheme.error)
            SettingsItem(
                icon = Icons.Default.DeleteForever,
                title = "Reset Local Data",
                subtitle = "Wipe all transactions on this device",
                titleColor = MaterialTheme.colorScheme.error,
                onClick = { showResetDialog = true }
            )

            SettingsItem(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = "Logout",
                subtitle = "Sign out and clear local cache",
                titleColor = MaterialTheme.colorScheme.error,
                onClick = {
                    transactionViewModel.resetAllData()
                    authViewModel.signOut()
                }
            )

            SettingsItem(
                icon = Icons.Default.PersonOff,
                title = "Delete Account",
                subtitle = "Permanently remove all data from cloud",
                titleColor = MaterialTheme.colorScheme.error,
                onClick = { showDeleteAccountDialog = true }
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

        // Dialogs
        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("Export Reports") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Choose what you want to export.", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = {
                                ExportHelper.exportToPDF(context, allTransactions, currency)
                                showExportDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PictureAsPdf, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Transactions (PDF)")
                        }
                        
                        Button(
                            onClick = {
                                ExportHelper.exportDebtsPDF(context, allDebts, currency)
                                showExportDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PictureAsPdf, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Debts & Loans (PDF)")
                        }
                        
                        Button(
                            onClick = {
                                ExportHelper.exportToExcelFriendly(context, allTransactions)
                                showExportDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.TableChart, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Export to Excel")
                        }
                        
                        OutlinedButton(
                            onClick = {
                                ExportHelper.exportToCSV(context, allTransactions)
                                showExportDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Description, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Transactions (CSV)")
                        }
                    }
                },
                confirmButton = {
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
                title = { Text("Reset Local Data?") },
                text = { Text("This will permanently delete all local transactions and budgets on this device. Cloud data remains safe.") },
                confirmButton = {
                    TextButton(onClick = {
                        transactionViewModel.resetAllData()
                        showResetDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar("Local data cleared")
                        }
                    }) {
                        Text("Reset Local", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showDeleteAccountDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAccountDialog = false },
                title = { Text("Delete Account Permanently?") },
                text = { Text("This action cannot be undone. All your cloud data and transactions will be deleted forever.") },
                confirmButton = {
                    TextButton(onClick = {
                        authViewModel.deleteAccount { success ->
                            if (success) {
                                transactionViewModel.resetAllData()
                                showDeleteAccountDialog = false
                            }
                        }
                    }) {
                        Text("Delete Everything", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAccountDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsSectionHeader(text: String, color: Color = MaterialTheme.colorScheme.primary) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = color,
        fontWeight = FontWeight.Bold
    )
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
