package com.nesh.planeazy.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nesh.planeazy.data.model.Constants
import com.nesh.planeazy.data.model.Transaction
import com.nesh.planeazy.data.model.TransactionType
import com.nesh.planeazy.ui.components.CategoryIcons
import com.nesh.planeazy.ui.components.DeleteConfirmationDialog
import com.nesh.planeazy.ui.viewmodel.TransactionViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: TransactionViewModel, 
    navController: NavController,
    transactionId: Long? = null,
    snackbarHostState: SnackbarHostState
) {
    var amount by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedSubCategory by remember { mutableStateOf("General") }
    var customCategoryName by remember { mutableStateOf("") }
    var selectedPaymentType by remember { mutableStateOf(Constants.PAYMENT_METHOD_TYPES[0]) }
    var paymentProvider by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedGoalId by remember { mutableStateOf<Long?>(null) }
    var goalsExpanded by remember { mutableStateOf(false) }
    var units by remember { mutableStateOf("") }

    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var currentTransaction by remember { mutableStateOf<Transaction?>(null) }
    
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val scope = rememberCoroutineScope()
    val goals by viewModel.allGoals.collectAsState()

    // Fetch transaction if editing
    LaunchedEffect(transactionId) {
        if (transactionId != null) {
            viewModel.getTransactionById(transactionId)?.let { t ->
                currentTransaction = t
                amount = t.amount.toString()
                title = t.title
                selectedType = t.type
                selectedCategory = t.category
                selectedSubCategory = t.subCategory ?: "General"
                selectedPaymentType = t.paymentMethodType
                paymentProvider = t.paymentMethodProvider
                note = t.note
                selectedGoalId = t.goalId
                units = t.units?.toString() ?: ""
                selectedDate = t.date
            }
        }
    }

    val categories = when (selectedType) {
        TransactionType.INCOME -> Constants.INCOME_CATEGORIES
        TransactionType.EXPENSE -> Constants.EXPENSE_CATEGORIES
        TransactionType.SAVINGS -> Constants.DEFAULT_GOAL_TYPES
    }

    LaunchedEffect(selectedType) {
        if (selectedCategory.isEmpty() || !categories.contains(selectedCategory)) {
            if (transactionId == null) {
                selectedCategory = categories[0]
            }
        }
    }

    LaunchedEffect(selectedCategory) {
        val subs = Constants.SUB_CATEGORIES[selectedCategory]
        if (transactionId == null) {
            selectedSubCategory = subs?.firstOrNull() ?: "General"
            units = "" 
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (transactionId == null) "Add Transaction" else "Edit Transaction") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (transactionId != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TransactionType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = TransactionType.entries.size)
                    ) {
                        Text(type.name)
                    }
                }
            }

            OutlinedTextField(
                value = amount,
                onValueChange = { input ->
                    if (input.isEmpty() || (input.all { char -> char.isDigit() || char == '.' } && input.count { it == '.' } <= 1)) {
                        amount = input
                    }
                },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title / Source (e.g., Monthly Rent)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = dateFormatter.format(Date(selectedDate)),
                onValueChange = {},
                label = { Text("Date") },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            )

            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            selectedDate = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                            showDatePicker = false
                        }) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Cancel")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            Text("Category", style = MaterialTheme.typography.labelLarge)
            CategoryDropdown(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )

            val subCategories = Constants.SUB_CATEGORIES[selectedCategory]
            if (subCategories != null) {
                Text("Sub-category", style = MaterialTheme.typography.labelLarge)
                CategoryDropdown(
                    categories = subCategories,
                    selectedCategory = selectedSubCategory,
                    onCategorySelected = { selectedSubCategory = it }
                )
            }

            val unitLabel = Constants.UNIT_MAPPING[selectedSubCategory]
            if (unitLabel != null) {
                OutlinedTextField(
                    value = units,
                    onValueChange = { input ->
                        if (input.isEmpty() || (input.all { char -> char.isDigit() || char == '.' } && input.count { it == '.' } <= 1)) {
                            units = input
                        }
                    },
                    label = { Text("Units used ($unitLabel)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("e.g., 12.5") }
                )
            }

            if (selectedCategory == "Custom" || selectedCategory == "Other") {
                OutlinedTextField(
                    value = customCategoryName,
                    onValueChange = { customCategoryName = it },
                    label = { Text("Enter Category Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("e.g., Crypto, Charity, Side Hustle") }
                )
            }

            Text("Payment Method Type", style = MaterialTheme.typography.labelLarge)
            CategoryDropdown(
                categories = Constants.PAYMENT_METHOD_TYPES,
                selectedCategory = selectedPaymentType,
                onCategorySelected = { selectedPaymentType = it }
            )

            OutlinedTextField(
                value = paymentProvider,
                onValueChange = { paymentProvider = it },
                label = { Text("Provider (e.g., M-Pesa, Equity, Cash)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (selectedType == TransactionType.SAVINGS) {
                Text("Link to Goal (Optional)", style = MaterialTheme.typography.labelLarge)
                ExposedDropdownMenuBox(
                    expanded = goalsExpanded,
                    onExpandedChange = { goalsExpanded = !goalsExpanded }
                ) {
                    OutlinedTextField(
                        value = goals.find { it.id == selectedGoalId }?.title ?: "No goal linked",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = goalsExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = goalsExpanded,
                        onDismissRequest = { goalsExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = {
                                selectedGoalId = null
                                goalsExpanded = false
                            }
                        )
                        goals.forEach { goal ->
                            DropdownMenuItem(
                                text = { Text(goal.title) },
                                onClick = {
                                    selectedGoalId = goal.id
                                    goalsExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Button(
                onClick = {
                    val amountVal = amount.toDoubleOrNull()
                    if (amountVal != null && title.isNotEmpty()) {
                        val finalCategory = if ((selectedCategory == "Custom" || selectedCategory == "Other") && customCategoryName.isNotEmpty()) {
                            customCategoryName
                        } else {
                            selectedCategory
                        }

                        val transaction = Transaction(
                            id = transactionId ?: 0,
                            amount = amountVal,
                            title = title,
                            date = selectedDate,
                            category = finalCategory,
                            subCategory = if (selectedSubCategory == "General") null else selectedSubCategory,
                            paymentMethodType = selectedPaymentType,
                            paymentMethodProvider = paymentProvider.ifEmpty { "Other" },
                            note = note,
                            type = selectedType,
                            goalId = selectedGoalId,
                            units = units.toDoubleOrNull()
                        )
                        
                        viewModel.addTransaction(transaction)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = amount.isNotEmpty() && title.isNotEmpty() && amount.toDoubleOrNull() != null
            ) {
                Text(if (transactionId == null) "Save Transaction" else "Update Transaction")
            }
        }
    }

    if (showDeleteDialog && currentTransaction != null) {
        DeleteConfirmationDialog(
            transaction = currentTransaction!!,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                val item = currentTransaction!!
                viewModel.deleteTransaction(item)
                showDeleteDialog = false
                navController.popBackStack()
                
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedCategory,
            onValueChange = {},
            readOnly = true,
            leadingIcon = {
                Icon(
                    imageVector = CategoryIcons.getIcon(selectedCategory),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = CategoryIcons.getIcon(category),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(category)
                        }
                    },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}
