package com.nesh.planeazy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nesh.planeazy.ui.components.TransactionItem
import com.nesh.planeazy.ui.components.EmptyState
import com.nesh.planeazy.ui.components.DeleteConfirmationDialog
import com.nesh.planeazy.ui.viewmodel.TransactionViewModel
import com.nesh.planeazy.data.model.Goal
import com.nesh.planeazy.data.model.Transaction
import com.nesh.planeazy.ui.navigation.Screen
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: TransactionViewModel, 
    navController: NavController,
    snackbarHostState: SnackbarHostState
) {
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val totalSavings by viewModel.totalSavings.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val goals by viewModel.allGoals.collectAsState()
    
    val balance = totalIncome - totalExpense
    var isBalanceVisible by remember { mutableStateOf(true) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                BalanceCard(balance, isBalanceVisible, onToggleVisibility = { isBalanceVisible = !isBalanceVisible })
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryCard("Income", totalIncome, Color(0xFF4CAF50), isBalanceVisible, Modifier.weight(1f))
                    SummaryCard("Expense", totalExpense, Color(0xFFF44336), isBalanceVisible, Modifier.weight(1f))
                    SummaryCard("Savings", totalSavings, Color(0xFF2196F3), isBalanceVisible, Modifier.weight(1f))
                }
            }

            if (goals.isNotEmpty()) {
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Goals Progress",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            TextButton(onClick = { 
                                navController.navigate(Screen.Goals.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }) {
                                Text("See All")
                                Icon(Icons.Default.ChevronRight, contentDescription = null)
                            }
                        }
                        
                        goals.take(2).forEach { goal ->
                            HomeGoalItem(goal)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            item {
                Text(
                    "Recent Transactions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (allTransactions.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.ReceiptLong,
                        title = "No transactions yet",
                        description = "Start your journey by adding your first transaction today.",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
                    )
                }
            } else {
                items(allTransactions.take(5)) { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        onClick = { 
                            navController.navigate("add_transaction?transactionId=${transaction.id}")
                        }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        FloatingActionButton(
            onClick = { navController.navigate("add_transaction") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Transaction")
        }
    }

    if (transactionToDelete != null) {
        DeleteConfirmationDialog(
            transaction = transactionToDelete!!,
            onDismiss = { transactionToDelete = null },
            onConfirm = {
                val item = transactionToDelete!!
                viewModel.deleteTransaction(item)
                transactionToDelete = null
                
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "Transaction deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.addTransaction(item)
                    }
                }
            }
        )
    }
}

@Composable
fun HomeGoalItem(goal: Goal) {
    val progress = if (goal.targetAmount > 0) (goal.savedAmount / goal.targetAmount).toFloat() else 0f
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(goal.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress.coerceAtMost(1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

@Composable
fun BalanceCard(balance: Double, isVisible: Boolean, onToggleVisibility: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Current Balance", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onToggleVisibility, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle Balance Visibility",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = if (isVisible) "KES ${String.format(Locale.getDefault(), "%.2f", balance)}" else "KES ••••••••",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SummaryCard(title: String, amount: Double, color: Color, isVisible: Boolean, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = color)
            Text(
                text = if (isVisible) "KES ${String.format(Locale.getDefault(), "%.0f", amount)}" else "KES ••••",
                fontWeight = FontWeight.Bold,
                color = color,
                fontSize = 14.sp
            )
        }
    }
}
