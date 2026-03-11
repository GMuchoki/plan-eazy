package com.example.plan_eazy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plan_eazy.data.model.Constants
import com.example.plan_eazy.data.model.Transaction
import com.example.plan_eazy.data.model.TransactionType
import com.example.plan_eazy.ui.viewmodel.TransactionViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: TransactionViewModel) {
    val allTransactions by viewModel.allTransactions.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    
    val categoryTotals = allTransactions
        .filter { it.type == TransactionType.EXPENSE }
        .groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    val paymentMethodTotals = allTransactions
        .groupBy { it.paymentMethodType }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()

    // Filter utility and resource data based on the centralized definitions in UNIT_MAPPING
    val resourceData = allTransactions
        .filter { it.subCategory != null && Constants.UNIT_MAPPING.containsKey(it.subCategory) }
        .groupBy { it.subCategory!! }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Reports & Analytics") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Spending Overview
            item {
                Text("Spending by Category", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            if (categoryTotals.isEmpty()) {
                item {
                    Text("No expense data available", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(categoryTotals) { (category, total) ->
                    val percentage = if (totalExpense > 0) (total / totalExpense).toFloat() else 0f
                    CategoryProgressItem(category, total, percentage)
                }
            }

            // Resource Consumption Section - Dynamically handles anything with a unit mapping
            if (resourceData.isNotEmpty()) {
                item {
                    HorizontalDivider()
                }
                item {
                    Text("Resource Consumption", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                items(resourceData.keys.toList()) { resourceName ->
                    val transactions = resourceData[resourceName] ?: emptyList()
                    ResourceConsumptionItem(resourceName, transactions)
                }
            }

            item {
                HorizontalDivider()
            }

            item {
                Text("Payment Method Usage", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            items(paymentMethodTotals) { (method, total) ->
                val totalFormatted = String.format(Locale.getDefault(), "%.2f", total)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(method)
                    Text("KES $totalFormatted", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ResourceConsumptionItem(resourceName: String, transactions: List<Transaction>) {
    val totalAmount = transactions.sumOf { it.amount }
    val totalUnits = transactions.sumOf { it.units ?: 0.0 }
    val unitLabel = Constants.UNIT_MAPPING[resourceName] ?: "units"
    val avgRate = if (totalUnits > 0) totalAmount / totalUnits else 0.0

    val unitsFormatted = String.format(Locale.getDefault(), "%.2f", totalUnits)
    val rateFormatted = String.format(Locale.getDefault(), "%.2f", avgRate)
    val amountFormatted = String.format(Locale.getDefault(), "%.2f", totalAmount)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(resourceName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Total Usage", style = MaterialTheme.typography.labelMedium)
                    Text("$unitsFormatted $unitLabel", fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    Text("Avg. Rate", style = MaterialTheme.typography.labelMedium)
                    Text("KES $rateFormatted / $unitLabel", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Total Spent: KES $amountFormatted",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CategoryProgressItem(category: String, amount: Double, percentage: Float) {
    val amountFormatted = String.format(Locale.getDefault(), "%.2f", amount)
    val percentFormatted = (percentage * 100).toInt()
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(category, fontSize = 14.sp)
            Text("KES $amountFormatted ($percentFormatted%)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { percentage.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}
