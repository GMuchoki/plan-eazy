package com.nesh.planeazy.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nesh.planeazy.data.model.*
import com.nesh.planeazy.data.model.Transaction as AppTransaction
import com.nesh.planeazy.ui.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(viewModel: TransactionViewModel, currency: String = "KES") {
    val debts by viewModel.allDebts.collectAsState()
    var showDebtDialog by remember { mutableStateOf(false) }
    var selectedDebt by remember { mutableStateOf<Debt?>(null) }
    var showPaymentSyncDialog by remember { mutableStateOf<Pair<Debt, Double>?>(null) }
    
    val totalIOwe = debts.filter { it.type == DebtType.OWED_BY_ME && it.status == "Active" }
        .sumOf { it.totalAmount - it.paidAmount }
    val totalOwedToMe = debts.filter { it.type == DebtType.OWED_TO_ME && it.status == "Active" }
        .sumOf { it.totalAmount - it.paidAmount }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                selectedDebt = null
                showDebtDialog = true 
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Debt")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DebtSummaryCard("You Owe", totalIOwe, Color(0xFFF44336), currency, Modifier.weight(1f))
                DebtSummaryCard("Owed to You", totalOwedToMe, Color(0xFF4CAF50), currency, Modifier.weight(1f))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("Active Debts & Loans", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            
            if (debts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No debts or loans tracked yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(debts) { debt ->
                        DebtItem(
                            debt = debt,
                            currency = currency,
                            onEdit = { 
                                selectedDebt = debt
                                showDebtDialog = true
                            },
                            onDelete = { viewModel.deleteDebt(debt) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        if (showDebtDialog) {
            DebtDialog(
                debt = selectedDebt,
                currency = currency,
                onDismiss = { showDebtDialog = false },
                onConfirm = { debt ->
                    val oldDebt = debts.find { it.id == debt.id }
                    val paymentMade = if (oldDebt != null) debt.paidAmount - oldDebt.paidAmount else 0.0
                    
                    viewModel.addDebt(debt)
                    showDebtDialog = false

                    // If a payment was recorded, ask to sync with transactions
                    if (paymentMade > 0) {
                        showPaymentSyncDialog = debt to paymentMade
                    }
                }
            )
        }

        if (showPaymentSyncDialog != null) {
            val (debt, amount) = showPaymentSyncDialog!!
            AlertDialog(
                onDismissRequest = { showPaymentSyncDialog = null },
                title = { Text("Add to Transactions?") },
                text = { Text("Would you like to record this payment of $currency ${String.format("%,.2f", amount)} as a transaction in your ledger?") },
                confirmButton = {
                    Button(onClick = {
                        val transactionType = if (debt.type == DebtType.OWED_BY_ME) TransactionType.EXPENSE else TransactionType.INCOME
                        val category = if (debt.type == DebtType.OWED_BY_ME) "Financial" else "Other"
                        val subCategory = if (debt.type == DebtType.OWED_BY_ME) "Loan / Debt" else null
                        
                        viewModel.addTransaction(
                            AppTransaction(
                                title = "Payment: ${debt.title}",
                                amount = amount,
                                type = transactionType,
                                category = category,
                                subCategory = subCategory,
                                date = System.currentTimeMillis(),
                                note = "Auto-generated from Debt payment",
                                paymentMethodType = "Other"
                            )
                        )
                        showPaymentSyncDialog = null
                    }) { Text("Yes, Add") }
                },
                dismissButton = {
                    TextButton(onClick = { showPaymentSyncDialog = null }) { Text("No") }
                }
            )
        }
    }
}

@Composable
fun DebtSummaryCard(title: String, amount: Double, color: Color, currency: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = color)
            Text(
                "$currency ${String.format(Locale.getDefault(), "%,.0f", amount)}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = color
            )
        }
    }
}

@Composable
fun DebtItem(debt: Debt, currency: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    val remaining = debt.totalAmount - debt.paidAmount
    val progress = if (debt.totalAmount > 0) (debt.paidAmount / debt.totalAmount).toFloat() else 0f
    val color = if (debt.type == DebtType.OWED_BY_ME) Color(0xFFF44336) else Color(0xFF4CAF50)
    val isSettled = debt.status == "Settled" || remaining <= 0

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(debt.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(debt.personName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(
                    imageVector = if (debt.type == DebtType.OWED_BY_ME) Icons.Default.ArrowOutward else Icons.AutoMirrored.Filled.CallReceived,
                    contentDescription = null,
                    tint = color
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = { progress.coerceAtMost(1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = color,
                strokeCap = StrokeCap.Round
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = if (isSettled) "Settled" else "Remaining: $currency ${String.format(Locale.getDefault(), "%,.0f", remaining)}",
                    fontSize = 12.sp,
                    fontWeight = if (isSettled) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSettled) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Total: $currency ${String.format(Locale.getDefault(), "%,.0f", debt.totalAmount)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtDialog(debt: Debt? = null, currency: String, onDismiss: () -> Unit, onConfirm: (Debt) -> Unit) {
    var title by remember { mutableStateOf(debt?.title ?: "") }
    var personName by remember { mutableStateOf(debt?.personName ?: "") }
    var totalAmount by remember { mutableStateOf(debt?.totalAmount?.toString() ?: "") }
    var paidAmount by remember { mutableStateOf(debt?.paidAmount?.toString() ?: "") }
    var selectedType by remember { mutableStateOf(debt?.type ?: DebtType.OWED_BY_ME) }
    var notes by remember { mutableStateOf(debt?.notes ?: "") }
    
    var selectedDate by remember { mutableLongStateOf(debt?.dueDate ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (debt == null) "Add Debt/Loan" else "Edit Debt/Loan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedType == DebtType.OWED_BY_ME,
                        onClick = { selectedType = DebtType.OWED_BY_ME },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text("I Owe", fontSize = 12.sp) }
                    SegmentedButton(
                        selected = selectedType == DebtType.OWED_TO_ME,
                        onClick = { selectedType = DebtType.OWED_TO_ME },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("Owed to Me", fontSize = 12.sp) }
                }

                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title (e.g., Car Loan)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = personName, onValueChange = { personName = it }, label = { Text("Person/Entity Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = totalAmount, onValueChange = { totalAmount = it }, label = { Text("Total Amount ($currency)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = paidAmount, onValueChange = { paidAmount = it }, label = { Text("Already Paid ($currency)") }, modifier = Modifier.fillMaxWidth())
                
                OutlinedTextField(
                    value = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(selectedDate)),
                    onValueChange = {},
                    label = { Text("Due Date") },
                    readOnly = true,
                    trailingIcon = { IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Default.DateRange, null) } },
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }
                )

                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (Optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val total = totalAmount.toDoubleOrNull() ?: 0.0
                    val paid = paidAmount.toDoubleOrNull() ?: 0.0
                    onConfirm(Debt(
                        id = debt?.id ?: 0,
                        title = title,
                        personName = personName,
                        totalAmount = total,
                        paidAmount = paid,
                        dueDate = selectedDate,
                        type = selectedType,
                        notes = notes,
                        status = if (paid >= total && total > 0) "Settled" else "Active"
                    ))
                },
                enabled = title.isNotEmpty() && personName.isNotEmpty() && totalAmount.isNotEmpty()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDate = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) { DatePicker(state = datePickerState) }
    }
}
