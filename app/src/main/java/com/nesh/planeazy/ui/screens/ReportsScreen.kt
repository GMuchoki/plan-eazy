package com.nesh.planeazy.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nesh.planeazy.data.model.Constants
import com.nesh.planeazy.data.model.Transaction
import com.nesh.planeazy.data.model.TransactionType
import com.nesh.planeazy.ui.components.CategoryIcons
import com.nesh.planeazy.ui.viewmodel.TransactionViewModel
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

    val resourceData = allTransactions
        .filter { it.subCategory != null && Constants.UNIT_MAPPING.containsKey(it.subCategory) }
        .groupBy { it.subCategory!! }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                if (categoryTotals.isNotEmpty()) {
                    SpendingDonutChart(categoryTotals, totalExpense)
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text("No spending data to visualize", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item {
                Text("Spending Breakdown", fontWeight = FontWeight.Bold, fontSize = 18.sp)
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

            if (resourceData.isNotEmpty()) {
                item {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Resource Consumption", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                items(resourceData.keys.toList()) { resourceName ->
                    val transactions = resourceData[resourceName] ?: emptyList()
                    ResourceConsumptionItem(resourceName, transactions)
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpendingDonutChart(categoryTotals: List<Pair<String, Double>>, totalExpense: Double) {
    val chartColors = listOf(
        Color(0xFF673AB7), Color(0xFF2196F3), Color(0xFF00BCD4),
        Color(0xFF4CAF50), Color(0xFFCDDC39), Color(0xFFFFEB3B),
        Color(0xFFFF9800), Color(0xFFF44336), Color(0xFFE91E63)
    )

    val animateFloat = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animateFloat.animateTo(1f, animationSpec = tween(durationMillis = 1000))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                Canvas(modifier = Modifier.size(180.dp)) {
                    var startAngle = -90f
                    categoryTotals.forEachIndexed { index, (_, amount) ->
                        val sweepAngle = ((amount / totalExpense) * 360f).toFloat() * animateFloat.value
                        drawArc(
                            color = chartColors[index % chartColors.size],
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = 30.dp.toPx(), cap = StrokeCap.Round)
                        )
                        startAngle += sweepAngle
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Spent", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "KES ${String.format(Locale.getDefault(), "%.0f", totalExpense)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Minimalist Legend
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 3
            ) {
                categoryTotals.take(6).forEachIndexed { index, (category, _) ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Surface(modifier = Modifier.size(8.dp), shape = RoundedCornerShape(2.dp), color = chartColors[index % chartColors.size]) {}
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(category, fontSize = 10.sp, maxLines = 1)
                    }
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
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = CategoryIcons.getIcon(resourceName),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(resourceName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Total Usage", style = MaterialTheme.typography.labelMedium)
                        Text("$unitsFormatted $unitLabel", fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
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
}

@Composable
fun CategoryProgressItem(category: String, amount: Double, percentage: Float) {
    val amountFormatted = String.format(Locale.getDefault(), "%.2f", amount)
    val percentFormatted = (percentage * 100).toInt()
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = CategoryIcons.getIcon(category),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text(category, fontSize = 14.sp)
                Text("KES $amountFormatted ($percentFormatted%)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { percentage.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                strokeCap = StrokeCap.Round
            )
        }
    }
}
