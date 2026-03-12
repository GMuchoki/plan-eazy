package com.nesh.planeazy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nesh.planeazy.data.model.Constants
import com.nesh.planeazy.data.model.Goal
import com.nesh.planeazy.ui.components.CategoryIcons
import com.nesh.planeazy.ui.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(viewModel: TransactionViewModel) {
    val goals by viewModel.allGoals.collectAsState()
    var showAddGoalDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Financial Goals") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddGoalDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Goal")
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
                Text("Your Targets", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }

            if (goals.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                            Text("No goals set yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(goals) { goal ->
                    GoalCard(goal, onDelete = { viewModel.deleteGoal(goal) })
                }
            }
        }

        if (showAddGoalDialog) {
            AddGoalDialog(
                onDismiss = { showAddGoalDialog = false },
                onConfirm = { goal ->
                    viewModel.addGoal(goal)
                    showAddGoalDialog = false
                }
            )
        }
    }
}

@Composable
fun GoalCard(goal: Goal, onDelete: () -> Unit) {
    val progress = if (goal.targetAmount > 0) (goal.savedAmount / goal.targetAmount).toFloat() else 0f
    val isCompleted = goal.savedAmount >= goal.targetAmount

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
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Saved: KES ${String.format("%.0f", goal.savedAmount)}", fontSize = 12.sp)
                Text("Target: KES ${String.format("%.0f", goal.targetAmount)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
fun AddGoalDialog(onDismiss: () -> Unit, onConfirm: (Goal) -> Unit) {
    var title by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(Constants.DEFAULT_GOAL_TYPES[0]) }
    var customTypeName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Goal") },
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
                    label = { Text("Target Amount") },
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
                            title = title,
                            type = finalType,
                            targetAmount = targetAmount.toDouble(),
                            savedAmount = 0.0
                        ))
                    }
                },
                enabled = title.isNotEmpty() && targetAmount.isNotEmpty()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
