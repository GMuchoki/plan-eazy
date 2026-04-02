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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nesh.planeazy.data.model.Constants
import com.nesh.planeazy.data.model.Goal
import com.nesh.planeazy.ui.components.CategoryIcons
import com.nesh.planeazy.ui.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(viewModel: TransactionViewModel, navController: NavController, currency: String = "KES") {
    val goals by viewModel.allGoals.collectAsState()
    var showGoalDialog by remember { mutableStateOf(false) }
    var selectedGoal by remember { mutableStateOf<Goal?>(null) }
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("All") }

    val filteredGoals = goals.filter { 
        (it.title.contains(searchQuery, ignoreCase = true) || it.type.contains(searchQuery, ignoreCase = true)) &&
        (selectedStatusFilter == "All" || 
         (selectedStatusFilter == "Active" && it.savedAmount < it.targetAmount) ||
         (selectedStatusFilter == "Completed" && it.savedAmount >= it.targetAmount))
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Financial Goals") })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    selectedGoal = null
                    showGoalDialog = true 
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Goal")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Search and Filter Header
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search goals...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedStatusFilter == "All",
                        onClick = { selectedStatusFilter = "All" },
                        label = { Text("All") }
                    )
                    FilterChip(
                        selected = selectedStatusFilter == "Active",
                        onClick = { selectedStatusFilter = "Active" },
                        label = { Text("Active") }
                    )
                    FilterChip(
                        selected = selectedStatusFilter == "Completed",
                        onClick = { selectedStatusFilter = "Completed" },
                        label = { Text("Completed") }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (filteredGoals.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxHeight(0.7f), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                Text(if (searchQuery.isEmpty()) "No goals set yet" else "No matching goals found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    items(filteredGoals) { goal ->
                        GoalCard(
                            goal = goal, 
                            currency = currency,
                            onDelete = { viewModel.deleteGoal(goal) },
                            onEdit = {
                                selectedGoal = goal
                                showGoalDialog = true
                            },
                            onContribute = {
                                navController.navigate("add_transaction?goalId=${goal.id}")
                            }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        if (showGoalDialog) {
            GoalDialog(
                goal = selectedGoal,
                currency = currency,
                onDismiss = { showGoalDialog = false },
                onConfirm = { goal ->
                    viewModel.addGoal(goal)
                    showGoalDialog = false
                }
            )
        }
    }
}

@Composable
fun GoalCard(goal: Goal, currency: String, onDelete: () -> Unit, onEdit: () -> Unit, onContribute: () -> Unit) {
    val progress = if (goal.targetAmount > 0) (goal.savedAmount / goal.targetAmount).toFloat() else 0f
    val isCompleted = goal.savedAmount >= goal.targetAmount

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = CategoryIcons.getIcon(goal.type),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(goal.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(goal.type, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LinearProgressIndicator(
                progress = { progress.coerceAtMost(1f) },
                modifier = Modifier.fillMaxWidth().height(12.dp),
                color = if (isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Saved: $currency ${String.format("%.0f", goal.savedAmount)}", fontSize = 12.sp)
                    Text("Target: $currency ${String.format("%.0f", goal.targetAmount)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                
                if (!isCompleted) {
                    Button(
                        onClick = onContribute,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Savings, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Contribute", fontSize = 12.sp)
                    }
                }
            }
            
            if (isCompleted) {
                Text(
                    "Goal Achieved! 🎉",
                    color = Color(0xFF4CAF50),
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
fun GoalDialog(goal: Goal? = null, currency: String, onDismiss: () -> Unit, onConfirm: (Goal) -> Unit) {
    var title by remember { mutableStateOf(goal?.title ?: "") }
    var targetAmount by remember { mutableStateOf(goal?.targetAmount?.toString() ?: "") }
    var selectedType by remember { mutableStateOf(if (goal != null && Constants.DEFAULT_GOAL_TYPES.contains(goal.type)) goal.type else if (goal != null) "Custom" else Constants.DEFAULT_GOAL_TYPES[0]) }
    var customTypeName by remember { mutableStateOf(if (goal != null && !Constants.DEFAULT_GOAL_TYPES.contains(goal.type)) goal.type else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (goal == null) "Create New Goal" else "Edit Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Goal Title (e.g., Dream Car)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Goal Type", style = MaterialTheme.typography.labelLarge)
                CategoryDropdown(
                    categories = Constants.DEFAULT_GOAL_TYPES,
                    selectedCategory = selectedType,
                    onCategorySelected = { selectedType = it }
                )

                // SHOW CUSTOM TYPE BOX IF "CUSTOM" IS SELECTED
                if (selectedType == "Custom") {
                    OutlinedTextField(
                        value = customTypeName,
                        onValueChange = { customTypeName = it },
                        label = { Text("Enter Custom Goal Type") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g., Wedding, Gadgets, etc.") }
                    )
                }

                OutlinedTextField(
                    value = targetAmount,
                    onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) targetAmount = it },
                    label = { Text("Target Amount ($currency)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotEmpty() && targetAmount.isNotEmpty()) {
                        val finalType = if (selectedType == "Custom" && customTypeName.isNotEmpty()) {
                            customTypeName
                        } else {
                            selectedType
                        }
                        
                        onConfirm(Goal(
                            id = goal?.id ?: 0,
                            title = title,
                            type = finalType,
                            targetAmount = targetAmount.toDouble(),
                            savedAmount = goal?.savedAmount ?: 0.0
                        ))
                    }
                },
                enabled = title.isNotEmpty() && targetAmount.isNotEmpty()
            ) {
                Text(if (goal == null) "Create" else "Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
