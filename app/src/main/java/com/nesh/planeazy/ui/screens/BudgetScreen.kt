package com.nesh.planeazy.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nesh.planeazy.data.model.Budget
import com.nesh.planeazy.data.model.Constants
import com.nesh.planeazy.data.model.TransactionType
import com.nesh.planeazy.data.model.UserCategory
import com.nesh.planeazy.ui.components.CategoryIcons
import com.nesh.planeazy.ui.viewmodel.TransactionViewModel
import com.nesh.planeazy.util.NotificationHelper
import com.nesh.planeazy.util.TransactionUtils
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(viewModel: TransactionViewModel, currency: String = "KES") {
    val budgets by viewModel.allBudgets.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val userCategories by viewModel.allUserCategories.collectAsState()
    
    var showBudgetDialog by remember { mutableStateOf(false) }
    var selectedBudget by remember { mutableStateOf<Budget?>(null) }
    
    var searchQuery by remember { mutableStateOf("") }

    val filteredBudgets = budgets.filter { 
        it.category.contains(searchQuery, ignoreCase = true) || 
        (it.subCategory?.contains(searchQuery, ignoreCase = true) == true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Search budgets...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (filteredBudgets.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxHeight(0.7f), contentAlignment = Alignment.Center) {
                            Text(if (searchQuery.isEmpty()) "No budgets set yet" else "No matching budgets found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(filteredBudgets) { budget ->
                        val spent = TransactionUtils.calculateSpentForBudget(
                            transactions = allTransactions,
                            category = budget.category,
                            subCategory = budget.subCategory,
                            month = budget.month,
                            year = budget.year
                        )
                        
                        BudgetCard(
                            budget = budget, 
                            spent = spent, 
                            currency = currency,
                            onDelete = { viewModel.deleteBudget(budget) },
                            onEdit = {
                                selectedBudget = budget
                                showBudgetDialog = true
                            }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        FloatingActionButton(
            onClick = { 
                selectedBudget = null
                showBudgetDialog = true 
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Set Budget")
        }

        if (showBudgetDialog) {
            BudgetDialog(
                budget = selectedBudget,
                currency = currency,
                userCategories = userCategories,
                onDismiss = { showBudgetDialog = false },
                onConfirm = { budget ->
                    viewModel.addBudget(budget)
                    showBudgetDialog = false
                }
            )
        }
    }
}

@Composable
fun BudgetCard(budget: Budget, spent: Double, currency: String, onDelete: () -> Unit, onEdit: () -> Unit) {
    val context = LocalContext.current
    val progress = if (budget.amount > 0) (spent / budget.amount).toFloat() else 0f
    val isOverBudget = spent > budget.amount
    val isNearLimit = progress >= 0.8f && !isOverBudget
    
    val color = when {
        isOverBudget -> MaterialTheme.colorScheme.error
        isNearLimit -> Color(0xFFFFA000) // Amber
        else -> MaterialTheme.colorScheme.primary
    }

    // Trigger Notification if budget is exceeded or near limit
    LaunchedEffect(progress) {
        if (isOverBudget) {
            NotificationHelper.showNotification(
                context, 
                "Budget Exceeded!", 
                "You've gone over your budget for ${budget.subCategory ?: budget.category}",
                budget.hashCode()
            )
        } else if (isNearLimit) {
            NotificationHelper.showNotification(
                context, 
                "Budget Alert", 
                "You've used 80% of your budget for ${budget.subCategory ?: budget.category}",
                budget.hashCode()
            )
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.05f)),
        border = if (isOverBudget || isNearLimit) androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = color.copy(alpha = 0.1f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = CategoryIcons.getIcon(budget.subCategory ?: budget.category),
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(budget.category, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        budget.subCategory?.let {
                            Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                
                Row {
                    if (isOverBudget || isNearLimit) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = color, modifier = Modifier.size(20.dp).align(Alignment.CenterVertically))
                        Spacer(Modifier.width(8.dp))
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LinearProgressIndicator(
                progress = { progress.coerceAtMost(1f) },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = color,
                trackColor = color.copy(alpha = 0.1f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val spentFormatted = String.format(Locale.getDefault(), "%,.0f", spent)
            val budgetFormatted = String.format(Locale.getDefault(), "%,.0f", budget.amount)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Spent: $currency $spentFormatted", fontSize = 12.sp, color = if (isOverBudget) color else MaterialTheme.colorScheme.onSurface)
                Text("Budget: $currency $budgetFormatted", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            
            if (isOverBudget) {
                val overFormatted = String.format(Locale.getDefault(), "%,.2f", spent - budget.amount)
                Text(
                    "Over budget by $currency $overFormatted",
                    color = color,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else if (isNearLimit) {
                Text(
                    "Warning: 80% limit reached",
                    color = color,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDialog(budget: Budget? = null, currency: String, userCategories: List<UserCategory>, onDismiss: () -> Unit, onConfirm: (Budget) -> Unit) {
    var amount by remember { mutableStateOf(budget?.amount?.toString() ?: "") }
    var selectedCategory by remember { mutableStateOf(budget?.category ?: Constants.EXPENSE_CATEGORIES[0]) }
    var selectedSubCategory by remember { mutableStateOf(budget?.subCategory ?: "General") }

    val expenseCategories = remember(userCategories) {
        (Constants.EXPENSE_CATEGORIES + userCategories.filter { it.type == TransactionType.EXPENSE && it.parentCategory == null }.map { it.name }).distinct()
    }

    val subCategories = remember(selectedCategory, userCategories) {
        val base = Constants.SUB_CATEGORIES[selectedCategory] ?: emptyList()
        val custom = userCategories.filter { it.parentCategory == selectedCategory }.map { it.name }
        (base + custom).distinct()
    }

    LaunchedEffect(selectedCategory) {
        if (budget == null || selectedCategory != budget.category) {
            selectedSubCategory = if (subCategories.isNotEmpty()) subCategories.first() else "General"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (budget == null) "Set Monthly Budget" else "Edit Budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Main Category", style = MaterialTheme.typography.labelLarge)
                CategoryDropdown(
                    categories = expenseCategories,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )

                if (subCategories.isNotEmpty() || selectedSubCategory != "General") {
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
                    label = { Text("Monthly Limit ($currency)") },
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
                            id = budget?.id ?: 0,
                            category = selectedCategory,
                            subCategory = if (selectedSubCategory == "General") null else selectedSubCategory,
                            amount = amountVal,
                            month = budget?.month ?: (calendar.get(Calendar.MONTH) + 1),
                            year = budget?.year ?: calendar.get(Calendar.YEAR)
                        ))
                    }
                },
                enabled = amount.isNotEmpty() && amount.toDoubleOrNull() != null
            ) {
                Text(if (budget == null) "Save" else "Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
