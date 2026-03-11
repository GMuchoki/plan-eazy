package com.example.plan_eazy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plan_eazy.data.model.Budget
import com.example.plan_eazy.data.model.Constants
import com.example.plan_eazy.data.model.TransactionType
import com.example.plan_eazy.ui.viewmodel.TransactionViewModel
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(viewModel: TransactionViewModel) {
    val budgets by viewModel.allBudgets.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Budgeting") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Set Budget")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Monthly Budgets", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }

            if (budgets.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No budgets set yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(budgets) { budget ->
                    val spent = allTransactions
                        .filter { 
                            it.type == TransactionType.EXPENSE && 
                            it.category == budget.category &&
                            (budget.subCategory == null || it.subCategory == budget.subCategory)
                        }
                        .sumOf { it.amount }
                    
                    BudgetCard(budget, spent, onDelete = { viewModel.deleteBudget(budget) })
                }
            }
        }

        if (showAddDialog) {
            AddBudgetDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { budget ->
                    viewModel.addBudget(budget)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun BudgetCard(budget: Budget, spent: Double, onDelete: () -> Unit) {
    val progress = if (budget.amount > 0) (spent / budget.amount).toFloat() else 0f
    val isOverBudget = spent > budget.amount
    val color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(budget.category, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    budget.subCategory?.let {
                        Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { progress.coerceAtMost(1f) },
                modifier = Modifier.fillMaxWidth().height(12.dp),
                color = color,
                trackColor = color.copy(alpha = 0.2f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val spentFormatted = String.format(Locale.getDefault(), "%.0f", spent)
            val budgetFormatted = String.format(Locale.getDefault(), "%.0f", budget.amount)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Spent: KES $spentFormatted", fontSize = 12.sp)
                Text("Budget: KES $budgetFormatted", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            
            if (isOverBudget) {
                val overFormatted = String.format(Locale.getDefault(), "%.2f", spent - budget.amount)
                Text(
                    "Over budget by KES $overFormatted",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetDialog(onDismiss: () -> Unit, onConfirm: (Budget) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(Constants.EXPENSE_CATEGORIES[0]) }
    var selectedSubCategory by remember { mutableStateOf("General") }

    val subCategories = Constants.SUB_CATEGORIES[selectedCategory]

    LaunchedEffect(selectedCategory) {
        selectedSubCategory = Constants.SUB_CATEGORIES[selectedCategory]?.firstOrNull() ?: "General"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Monthly Budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Main Category", style = MaterialTheme.typography.labelLarge)
                CategoryDropdown(
                    categories = Constants.EXPENSE_CATEGORIES,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )

                if (subCategories != null) {
                    Text("Sub-category", style = MaterialTheme.typography.labelLarge)
                    CategoryDropdown(
                        categories = listOf("General") + subCategories,
                        selectedCategory = selectedSubCategory,
                        onCategorySelected = { selectedSubCategory = it }
                    )
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { input ->
                        if (input.isEmpty() || (input.all { char -> char.isDigit() || char == '.' } && input.count { it == '.' } <= 1)) {
                            amount = input
                        }
                    },
                    label = { Text("Monthly Limit (KES)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountVal = amount.toDoubleOrNull()
                    if (amountVal != null) {
                        val calendar = Calendar.getInstance()
                        onConfirm(Budget(
                            category = selectedCategory,
                            subCategory = if (selectedSubCategory == "General") null else selectedSubCategory,
                            amount = amountVal,
                            month = calendar.get(Calendar.MONTH) + 1,
                            year = calendar.get(Calendar.YEAR)
                        ))
                    }
                },
                enabled = amount.isNotEmpty() && amount.toDoubleOrNull() != null
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
