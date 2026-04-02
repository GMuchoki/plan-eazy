package com.nesh.planeazy.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.nesh.planeazy.ui.components.TransactionItem
import com.nesh.planeazy.ui.components.EmptyState
import com.nesh.planeazy.ui.components.DeleteConfirmationDialog
import com.nesh.planeazy.ui.viewmodel.TransactionViewModel
import com.nesh.planeazy.data.model.*
import com.nesh.planeazy.ui.navigation.Screen
import com.nesh.planeazy.ui.components.CategoryIcons
import com.nesh.planeazy.util.TransactionUtils
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: TransactionViewModel, 
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    currency: String = "KES"
) {
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val totalSavings by viewModel.totalSavings.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val goals by viewModel.allGoals.collectAsState()
    val budgets by viewModel.allBudgets.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    
    val balance = totalIncome - totalExpense
    var isBalanceVisible by remember { mutableStateOf(true) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    
    val scope = rememberCoroutineScope()

    val onNavigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Dashboard",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val syncText = if (isSyncing) "Syncing..." else "Synced to Cloud"
                            val syncColor = if (isSyncing) MaterialTheme.colorScheme.primary else Color(0xFF4CAF50)
                            Icon(
                                if (isSyncing) Icons.Default.Sync else Icons.Default.CloudDone,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = syncColor
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(syncText, style = MaterialTheme.typography.labelSmall, color = syncColor)
                        }
                    }
                }
            }

            item {
                BalanceCard(balance, isBalanceVisible, currency, onToggleVisibility = { isBalanceVisible = !isBalanceVisible })
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryCard("Income", totalIncome, Color(0xFF4CAF50), isBalanceVisible, currency, Modifier.weight(1f))
                    SummaryCard("Expense", totalExpense, Color(0xFFF44336), isBalanceVisible, currency, Modifier.weight(1f))
                    SummaryCard("Savings", totalSavings, Color(0xFF2196F3), isBalanceVisible, currency, Modifier.weight(1f))
                }
            }

            // Top Budgets Section - Corrected with Date Filtering
            if (budgets.isNotEmpty()) {
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Top Budgets", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            TextButton(onClick = { onNavigateToTab(Screen.Budget.route) }) {
                                Text("View All")
                                Icon(Icons.Default.ChevronRight, null)
                            }
                        }
                        budgets.take(3).forEach { budget ->
                            val spent = TransactionUtils.calculateSpentForBudget(
                                transactions = allTransactions,
                                category = budget.category,
                                subCategory = budget.subCategory,
                                month = budget.month,
                                year = budget.year
                            )
                            HomeBudgetGlanceItem(budget, spent, currency)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
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
                                onNavigateToTab(Screen.Goals.route)
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
                        currency = currency,
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
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
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
fun HomeBudgetGlanceItem(budget: Budget, spent: Double, currency: String) {
    val progress = if (budget.amount > 0) (spent / budget.amount).toFloat() else 0f
    val isOver = spent > budget.amount
    val isNear = progress >= 0.8f && !isOver
    val color = if (isOver) MaterialTheme.colorScheme.error else if (isNear) Color(0xFFFFA000) else MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = CategoryIcons.getIcon(budget.subCategory ?: budget.category),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(budget.subCategory ?: budget.category, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text("$currency ${String.format(Locale.getDefault(), "%,.0f", spent)} / ${String.format(Locale.getDefault(), "%,.0f", budget.amount)}", fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress.coerceAtMost(1f) },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = color,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
            if (isOver || isNear) {
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.Warning, null, tint = color, modifier = Modifier.size(16.dp))
            }
        }
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
fun BalanceCard(balance: Double, isVisible: Boolean, currency: String, onToggleVisibility: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Current Balance", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onToggleVisibility, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle Balance Visibility",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Text(
                text = if (isVisible) "$currency ${String.format(Locale.getDefault(), "%,.2f", balance)}" else "$currency ••••••••",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun SummaryCard(title: String, amount: Double, color: Color, isVisible: Boolean, currency: String, modifier: Modifier = Modifier) {
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
                text = if (isVisible) "$currency ${String.format(Locale.getDefault(), "%,.0f", amount)}" else "$currency ••••",
                fontWeight = FontWeight.Bold,
                color = color,
                fontSize = 14.sp
            )
        }
    }
}
