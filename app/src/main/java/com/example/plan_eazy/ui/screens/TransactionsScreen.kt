package com.example.plan_eazy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.plan_eazy.data.model.Constants
import com.example.plan_eazy.data.model.TransactionType
import com.example.plan_eazy.ui.components.TransactionItem
import com.example.plan_eazy.ui.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionsScreen(viewModel: TransactionViewModel) {
    val allTransactions by viewModel.allTransactions.collectAsState()
    var selectedTypeFilter by remember { mutableStateOf<TransactionType?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    
    val filteredTransactions = allTransactions.filter { 
        (selectedTypeFilter == null || it.type == selectedTypeFilter) &&
        (selectedCategoryFilter == null || it.category == selectedCategoryFilter) &&
        (searchQuery.isEmpty() || it.title.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true))
    }

    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Transactions") },
                    actions = {
                        IconButton(onClick = { showFilterSheet = true }) {
                            Box {
                                Icon(Icons.Default.FilterList, contentDescription = "Filter")
                                if (selectedCategoryFilter != null) {
                                    Badge(modifier = Modifier.offset(x = 8.dp, y = (-8).dp))
                                }
                            }
                        }
                    }
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search transactions...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ScrollableTabRow(
                selectedTabIndex = when (selectedTypeFilter) {
                    null -> 0
                    TransactionType.INCOME -> 1
                    TransactionType.EXPENSE -> 2
                    TransactionType.SAVINGS -> 3
                },
                edgePadding = 16.dp,
                divider = {}
            ) {
                Tab(selected = selectedTypeFilter == null, onClick = { selectedTypeFilter = null }) {
                    Text("All", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = selectedTypeFilter == TransactionType.INCOME, onClick = { selectedTypeFilter = TransactionType.INCOME }) {
                    Text("Income", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = selectedTypeFilter == TransactionType.EXPENSE, onClick = { selectedTypeFilter = TransactionType.EXPENSE }) {
                    Text("Expenses", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = selectedTypeFilter == TransactionType.SAVINGS, onClick = { selectedTypeFilter = TransactionType.SAVINGS }) {
                    Text("Savings", modifier = Modifier.padding(16.dp))
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredTransactions) { transaction ->
                    var showDeleteDialog by remember { mutableStateOf(false) }
                    
                    TransactionItem(
                        transaction = transaction,
                        onClick = { showDeleteDialog = true }
                    )

                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Delete Transaction") },
                            text = { Text("Are you sure you want to delete this transaction?") },
                            confirmButton = {
                                TextButton(onClick = {
                                    viewModel.deleteTransaction(transaction)
                                    showDeleteDialog = false
                                }) {
                                    Text("Delete", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
                
                if (filteredTransactions.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            Text("No transactions found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
                ) {
                    Text("Filter by Category", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategoryFilter == null,
                            onClick = { selectedCategoryFilter = null },
                            label = { Text("All Categories") }
                        )
                        Constants.EXPENSE_CATEGORIES.forEach { category ->
                            FilterChip(
                                selected = selectedCategoryFilter == category,
                                onClick = { selectedCategoryFilter = category },
                                label = { Text(category) }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showFilterSheet = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Apply Filters")
                    }
                }
            }
        }
    }
}
