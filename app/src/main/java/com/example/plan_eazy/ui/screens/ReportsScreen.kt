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
import com.example.plan_eazy.data.model.TransactionType
import com.example.plan_eazy.ui.viewmodel.TransactionViewModel

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

            item {
                HorizontalDivider()
            }

            item {
                Text("Payment Method Usage", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            items(paymentMethodTotals) { (method, total) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(method)
                    Text("KES ${String.format("%.2f", total)}", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CategoryProgressItem(category: String, amount: Double, percentage: Float) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(category, fontSize = 14.sp)
            Text("KES ${String.format("%.2f", amount)} (${(percentage * 100).toInt()}%)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { percentage },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}
