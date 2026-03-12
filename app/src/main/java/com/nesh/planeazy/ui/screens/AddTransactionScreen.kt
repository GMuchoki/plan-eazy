package com.nesh.planeazy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.nesh.planeazy.ui.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(viewModel: TransactionViewModel, navController: NavController) {
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

    val goals by viewModel.allGoals.collectAsState()

    val categories = when (selectedType) {
        TransactionType.INCOME -> Constants.INCOME_CATEGORIES
        TransactionType.EXPENSE -> Constants.EXPENSE_CATEGORIES
        TransactionType.SAVINGS -> Constants.DEFAULT_GOAL_TYPES
    }

    LaunchedEffect(selectedType) {
        if (selectedCategory.isEmpty() || !categories.contains(selectedCategory)) {
            selectedCategory = categories[0]
        }
    }

    LaunchedEffect(selectedCategory) {
        val subs = Constants.SUB_CATEGORIES[selectedCategory]
        selectedSubCategory = subs?.firstOrNull() ?: "General"
        units = "" // Reset units when category changes
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Transaction") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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

            Text("Category", style = MaterialTheme.typography.labelLarge)
            CategoryDropdown(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )

            // DYNAMIC SUB-CATEGORY SELECTOR
            val subCategories = Constants.SUB_CATEGORIES[selectedCategory]
            if (subCategories != null) {
                Text("Sub-category", style = MaterialTheme.typography.labelLarge)
                CategoryDropdown(
                    categories = subCategories,
                    selectedCategory = selectedSubCategory,
                    onCategorySelected = { selectedSubCategory = it }
                )
            }

            // DYNAMIC UNITS FIELD BASED ON SUB-CATEGORY
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

            // SHOW CUSTOM CATEGORY FIELD IF "CUSTOM" OR "OTHER" IS SELECTED
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

                        viewModel.addTransaction(
                            Transaction(
                                amount = amountVal,
                                title = title,
                                date = System.currentTimeMillis(),
                                category = finalCategory,
                                subCategory = if (selectedSubCategory == "General") null else selectedSubCategory,
                                paymentMethodType = selectedPaymentType,
                                paymentMethodProvider = paymentProvider.ifEmpty { "Other" },
                                note = note,
                                type = selectedType,
                                goalId = selectedGoalId,
                                units = units.toDoubleOrNull()
                            )
                        )
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = amount.isNotEmpty() && title.isNotEmpty() && amount.toDoubleOrNull() != null
            ) {
                Text("Save Transaction")
            }
        }
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
