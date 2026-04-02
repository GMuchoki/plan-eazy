package com.nesh.planeazy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nesh.planeazy.data.model.Constants
import com.nesh.planeazy.data.model.TransactionType
import com.nesh.planeazy.data.model.UserCategory
import com.nesh.planeazy.ui.components.CategoryIcons
import com.nesh.planeazy.ui.viewmodel.TransactionViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(viewModel: TransactionViewModel, navController: NavController) {
    val userCategories by viewModel.allUserCategories.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    
    var categoryToDelete by remember { mutableStateOf<UserCategory?>(null) }
    var usageCount by remember { mutableIntStateOf(0) }
    var budgetCount by remember { mutableIntStateOf(0) }
    
    val scope = rememberCoroutineScope()

    val defaultCategories = if (selectedType == TransactionType.EXPENSE) Constants.EXPENSE_CATEGORIES else Constants.INCOME_CATEGORIES
    val customCategories = userCategories.filter { it.type == selectedType }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Categories") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Category")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = if (selectedType == TransactionType.EXPENSE) 0 else 1) {
                Tab(selected = selectedType == TransactionType.EXPENSE, onClick = { selectedType = TransactionType.EXPENSE }) {
                    Text("Expenses", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = selectedType == TransactionType.INCOME, onClick = { selectedType = TransactionType.INCOME }) {
                    Text("Income", modifier = Modifier.padding(16.dp))
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text("Default Categories", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
                items(defaultCategories) { name ->
                    CategoryItem(name = name, isDefault = true)
                }

                if (customCategories.isNotEmpty()) {
                    item { Spacer(Modifier.height(16.dp)) }
                    item { Text("Your Custom Categories", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary) }
                    items(customCategories) { category ->
                        CategoryItem(
                            name = category.name, 
                            parentName = category.parentCategory,
                            isDefault = false, 
                            onDelete = { 
                                scope.launch {
                                    usageCount = viewModel.getTransactionCountForCategory(category.name)
                                    budgetCount = viewModel.getBudgetCountForCategory(category.name)
                                    categoryToDelete = category
                                }
                            }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        if (showAddDialog) {
            AddCategoryDialog(
                type = selectedType,
                existingMainCategories = defaultCategories + userCategories.filter { it.type == selectedType && it.parentCategory == null }.map { it.name },
                onDismiss = { showAddDialog = false },
                onConfirm = { name, parent ->
                    viewModel.addUserCategory(UserCategory(name = name, parentCategory = parent, type = selectedType))
                    showAddDialog = false
                }
            )
        }

        if (categoryToDelete != null) {
            val isSub = categoryToDelete!!.parentCategory != null
            val totalUsage = usageCount + budgetCount
            
            AlertDialog(
                onDismissRequest = { categoryToDelete = null },
                title = { Text("Delete Category?") },
                text = { 
                    Column {
                        Text("Are you sure you want to delete '${categoryToDelete!!.name}'?")
                        if (totalUsage > 0) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Warning: This category is used in $usageCount transactions and $budgetCount budgets. " +
                                "Deleting it will move them to 'Other' or 'General'.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteUserCategoryWithReassignment(categoryToDelete!!)
                            categoryToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete & Reassign") }
                },
                dismissButton = {
                    TextButton(onClick = { categoryToDelete = null }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun CategoryItem(name: String, parentName: String? = null, isDefault: Boolean, onDelete: (() -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = CategoryIcons.getIcon(name), 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(name, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                    if (parentName != null) {
                        Text("Sub-category of $parentName", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (!isDefault && onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryDialog(type: TransactionType, existingMainCategories: List<String>, onDismiss: () -> Unit, onConfirm: (String, String?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var isSubCategory by remember { mutableStateOf(false) }
    var selectedParent by remember { mutableStateOf(existingMainCategories.firstOrNull() ?: "") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add ${type.name.lowercase().replaceFirstChar { it.uppercase() }} Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isSubCategory, onCheckedChange = { isSubCategory = it })
                    Text("This is a sub-category")
                }

                if (isSubCategory) {
                    Text("Select Parent Category", style = MaterialTheme.typography.labelSmall)
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        OutlinedTextField(
                            value = selectedParent,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            existingMainCategories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        selectedParent = category
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, if (isSubCategory) selectedParent else null) }, enabled = name.isNotBlank()) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
