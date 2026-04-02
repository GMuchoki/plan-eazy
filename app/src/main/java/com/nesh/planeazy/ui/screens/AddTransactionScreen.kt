package com.nesh.planeazy.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.nesh.planeazy.data.model.Constants
import com.nesh.planeazy.data.model.Transaction
import com.nesh.planeazy.data.model.TransactionType
import com.nesh.planeazy.ui.components.CategoryIcons
import com.nesh.planeazy.ui.components.DeleteConfirmationDialog
import com.nesh.planeazy.ui.viewmodel.TransactionViewModel
import com.nesh.planeazy.util.TransactionUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: TransactionViewModel, 
    navController: NavController,
    transactionId: Long? = null,
    snackbarHostState: SnackbarHostState,
    initialGoalId: Long? = null,
    currency: String = "KES"
) {
    var amount by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(if (initialGoalId != null) TransactionType.SAVINGS else TransactionType.EXPENSE) }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedSubCategory by remember { mutableStateOf("General") }
    var customCategoryName by remember { mutableStateOf("") }
    var selectedPaymentType by remember { mutableStateOf(Constants.PAYMENT_METHOD_TYPES[0]) }
    var paymentProvider by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedGoalId by remember { mutableStateOf<Long?>(initialGoalId) }
    var goalsExpanded by remember { mutableStateOf(false) }
    var units by remember { mutableStateOf("") }
    
    // Recurring & Attachment fields
    var isRecurring by remember { mutableStateOf(false) }
    var frequency by remember { mutableStateOf("Monthly") }
    var attachmentUri by remember { mutableStateOf<Uri?>(null) }

    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var currentTransaction by remember { mutableStateOf<Transaction?>(null) }
    
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val scope = rememberCoroutineScope()
    val goals by viewModel.allGoals.collectAsState()
    val userCategories by viewModel.allUserCategories.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        attachmentUri = uri
    }

    // Fetch transaction if editing
    LaunchedEffect(transactionId) {
        transactionId?.let { id ->
            viewModel.getTransactionById(id)?.let { t ->
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
                isRecurring = t.isRecurring
                frequency = t.frequency ?: "Monthly"
                attachmentUri = t.attachmentUri?.let { Uri.parse(it) }
            }
        }
    }

    val categories = remember(selectedType, userCategories) {
        val base = when (selectedType) {
            TransactionType.INCOME -> Constants.INCOME_CATEGORIES
            TransactionType.EXPENSE -> Constants.EXPENSE_CATEGORIES
            TransactionType.SAVINGS -> Constants.DEFAULT_GOAL_TYPES
        }
        val custom = userCategories
            .filter { it.type == selectedType && it.parentCategory == null }
            .map { it.name }
        (base + custom).distinct()
    }

    LaunchedEffect(selectedType) {
        if (selectedCategory.isEmpty() || !categories.contains(selectedCategory)) {
            if (transactionId == null) {
                val targetCat = if (initialGoalId != null && selectedType == TransactionType.SAVINGS) {
                    goals.find { it.id == initialGoalId }?.type ?: categories[0]
                } else {
                    categories[0]
                }
                selectedCategory = if (categories.contains(targetCat)) targetCat else categories[0]
            }
        }
    }

    LaunchedEffect(selectedCategory) {
        val baseSubs = Constants.SUB_CATEGORIES[selectedCategory] ?: emptyList()
        val customSubs = userCategories
            .filter { it.parentCategory == selectedCategory }
            .map { it.name }
        val subs = (baseSubs + customSubs).distinct()

        if (transactionId == null) {
            selectedSubCategory = if (subs.isNotEmpty()) subs.first() else "General"
            units = "" 
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (transactionId == null) "Add Transaction" else "Edit Transaction") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                label = { Text("Amount ($currency)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
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
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            // Recurring Options
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isRecurring, onCheckedChange = { isRecurring = it })
                Text("Recurring Transaction")
            }

            if (isRecurring) {
                CategoryDropdown(
                    categories = listOf("Daily", "Weekly", "Monthly", "Yearly"),
                    selectedCategory = frequency,
                    onCategorySelected = { frequency = it }
                )
            }

            Text("Category", style = MaterialTheme.typography.labelLarge)
            CategoryDropdown(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )

            val baseSubs = Constants.SUB_CATEGORIES[selectedCategory] ?: emptyList()
            val customSubs = userCategories
                .filter { it.parentCategory == selectedCategory }
                .map { it.name }
            val subCategories = (baseSubs + customSubs).distinct()

            if (subCategories.isNotEmpty()) {
                Text("Sub-category", style = MaterialTheme.typography.labelLarge)
                CategoryDropdown(
                    categories = subCategories,
                    selectedCategory = selectedSubCategory,
                    onCategorySelected = { selectedSubCategory = it }
                )
            }

            // Attachment Section
            Text("Attachment", style = MaterialTheme.typography.labelLarge)
            Card(
                modifier = Modifier.fillMaxWidth().height(150.dp).clickable { launcher.launch("image/*") },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                if (attachmentUri != null) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = attachmentUri,
                            contentDescription = "Receipt Attachment",
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { attachmentUri = null },
                            modifier = Modifier.align(Alignment.TopEnd).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White)
                        }
                    }
                } else {
                    val context = LocalContext.current
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(48.dp))
                        Text("Add Receipt Photo", style = MaterialTheme.typography.bodySmall)
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
                            units = units.toDoubleOrNull(),
                            isRecurring = isRecurring,
                            frequency = if (isRecurring) frequency else null,
                            nextOccurrence = if (isRecurring) TransactionUtils.calculateNextOccurrence(selectedDate, frequency) else null,
                            isTemplate = isRecurring && transactionId == null,
                            attachmentUri = attachmentUri?.toString()
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
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CategoryIcons.getIcon(category).let {
                                Icon(it, contentDescription = null, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                            }
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
