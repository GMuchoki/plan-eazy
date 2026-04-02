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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: TransactionViewModel, currency: String = "KES") {
    val allTransactions by viewModel.allTransactions.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("Spending", modifier = Modifier.padding(16.dp))
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("Income", modifier = Modifier.padding(16.dp))
            }
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                Text("Trends", modifier = Modifier.padding(16.dp))
            }
            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }) {
                Text("Resources", modifier = Modifier.padding(16.dp))
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> SpendingTab(allTransactions, totalExpense, currency)
                1 -> IncomeTab(allTransactions, totalIncome, currency)
                2 -> TrendsTab(allTransactions, currency)
                3 -> ResourcesTab(allTransactions, currency)
            }
        }
    }
}

@Composable
fun SpendingTab(allTransactions: List<Transaction>, totalExpense: Double, currency: String) {
    val categoryTotals = allTransactions
        .filter { it.type == TransactionType.EXPENSE }
        .groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            if (categoryTotals.isNotEmpty()) {
                SummaryDonutChart(categoryTotals, totalExpense, currency, "Total Spent")
            } else {
                EmptyDataState("No spending data to visualize")
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
                CategoryProgressItem(category, total, percentage, currency, Color(0xFFF44336))
            }
        }
        
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
fun IncomeTab(allTransactions: List<Transaction>, totalIncome: Double, currency: String) {
    val categoryTotals = allTransactions
        .filter { it.type == TransactionType.INCOME }
        .groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            if (categoryTotals.isNotEmpty()) {
                SummaryDonutChart(categoryTotals, totalIncome, currency, "Total Income")
            } else {
                EmptyDataState("No income data to visualize")
            }
        }

        item {
            Text("Income Breakdown", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        if (categoryTotals.isEmpty()) {
            item {
                Text("No income data available", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(categoryTotals) { (category, total) ->
                val percentage = if (totalIncome > 0) (total / totalIncome).toFloat() else 0f
                CategoryProgressItem(category, total, percentage, currency, Color(0xFF4CAF50))
            }
        }
        
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
fun TrendsTab(allTransactions: List<Transaction>, currency: String) {
    val calendar = Calendar.getInstance()
    val monthData = mutableListOf<MonthTrend>()

    for (i in 5 downTo 0) {
        val tempCal = calendar.clone() as Calendar
        tempCal.add(Calendar.MONTH, -i)
        val month = tempCal.get(Calendar.MONTH)
        val year = tempCal.get(Calendar.YEAR)
        
        val monthTransactions = allTransactions.filter { t ->
            val tCal = Calendar.getInstance().apply { timeInMillis = t.date }
            tCal.get(Calendar.MONTH) == month && tCal.get(Calendar.YEAR) == year
        }

        val income = monthTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = monthTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val label = SimpleDateFormat("MMM", Locale.getDefault()).format(tempCal.time)
        
        monthData.add(MonthTrend(label, income, expense))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text("Income vs Expenses (Last 6 Months)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))
            TrendsBarChart(monthData, currency)
        }

        item {
            Text("Monthly Summary", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        items(monthData.reversed()) { data ->
            TrendSummaryItem(data, currency)
        }
        
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
fun ResourcesTab(allTransactions: List<Transaction>, currency: String) {
    val resourceData = allTransactions
        .filter { it.subCategory != null && Constants.UNIT_MAPPING.containsKey(it.subCategory) }
        .groupBy { it.subCategory!! }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }
        if (resourceData.isEmpty()) {
            item { EmptyDataState("No resource usage data found") }
        } else {
            items(resourceData.keys.toList()) { resourceName ->
                val transactions = resourceData[resourceName] ?: emptyList()
                ResourceConsumptionItem(resourceName, transactions, currency)
            }
        }
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

data class MonthTrend(val label: String, val income: Double, val expense: Double)

@Composable
fun TrendsBarChart(data: List<MonthTrend>, currency: String) {
    val maxVal = (data.maxOfOrNull { maxOf(it.income, it.expense) } ?: 100.0).coerceAtLeast(1.0)
    val incomeColor = Color(0xFF4CAF50)
    val expenseColor = Color(0xFFF44336)

    Card(
        modifier = Modifier.fillMaxWidth().height(300.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                LegendItem("Income", incomeColor)
                Spacer(modifier = Modifier.width(16.dp))
                LegendItem("Expense", expenseColor)
            }

            Canvas(modifier = Modifier.fillMaxSize().weight(1f)) {
                val width = size.width
                val height = size.height
                val barGap = 24.dp.toPx()
                val groupWidth = (width - (data.size - 1) * barGap) / data.size
                val barWidth = groupWidth / 2.5f

                data.forEachIndexed { index, trend ->
                    val xBase = index * (groupWidth + barGap)
                    
                    // Income Bar
                    val incomeHeight = (trend.income / maxVal).toFloat() * height
                    drawRect(
                        color = incomeColor,
                        topLeft = Offset(xBase + (groupWidth / 2) - barWidth, height - incomeHeight),
                        size = Size(barWidth, incomeHeight)
                    )

                    // Expense Bar
                    val expenseHeight = (trend.expense / maxVal).toFloat() * height
                    drawRect(
                        color = expenseColor,
                        topLeft = Offset(xBase + (groupWidth / 2), height - expenseHeight),
                        size = Size(barWidth, expenseHeight)
                    )
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                data.forEach { trend ->
                    Text(
                        text = trend.label,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(12.dp), shape = RoundedCornerShape(2.dp), color = color) {}
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 12.sp)
    }
}

@Composable
fun TrendSummaryItem(trend: MonthTrend, currency: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(trend.label, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.width(50.dp))
            
            Column(horizontalAlignment = Alignment.End) {
                Text("Income", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                Text("$currency ${String.format(Locale.getDefault(), "%,.0f", trend.income)}", fontWeight = FontWeight.Medium)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("Expenses", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF44336))
                Text("$currency ${String.format(Locale.getDefault(), "%,.0f", trend.expense)}", fontWeight = FontWeight.Medium)
            }

            val net = trend.income - trend.expense
            Column(horizontalAlignment = Alignment.End) {
                Text("Net", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = "$currency ${String.format(Locale.getDefault(), "%,.0f", net)}",
                    fontWeight = FontWeight.Bold,
                    color = if (net >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
        }
    }
}

@Composable
fun EmptyDataState(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SummaryDonutChart(categoryTotals: List<Pair<String, Double>>, total: Double, currency: String, title: String) {
    val chartColors = listOf(
        Color(0xFF673AB7), Color(0xFF2196F3), Color(0xFF00BCD4),
        Color(0xFF4CAF50), Color(0xFFCDDC39), Color(0xFFFFEB3B),
        Color(0xFFFF9800), Color(0xFFF44336), Color(0xFFE91E63)
    )

    val animateFloat = remember { Animatable(0f) }
    LaunchedEffect(total) {
        animateFloat.snapTo(0f)
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
                        val sweepAngle = ((amount / total) * 360f).toFloat() * animateFloat.value
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
                    Text(title, style = MaterialTheme.typography.labelMedium)
                    Text(
                        "$currency ${String.format(Locale.getDefault(), "%,.0f", total)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
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
fun ResourceConsumptionItem(resourceName: String, transactions: List<Transaction>, currency: String) {
    val totalAmount = transactions.sumOf { it.amount }
    val totalUnits = transactions.sumOf { it.units ?: 0.0 }
    val unitLabel = Constants.UNIT_MAPPING[resourceName] ?: "units"
    val avgRate = if (totalUnits > 0) totalAmount / totalUnits else 0.0

    val unitsFormatted = String.format(Locale.getDefault(), "%,.2f", totalUnits)
    val rateFormatted = String.format(Locale.getDefault(), "%,.2f", avgRate)
    val amountFormatted = String.format(Locale.getDefault(), "%,.2f", totalAmount)

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
                        Text("$currency $rateFormatted / $unitLabel", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Total Spent: $currency $amountFormatted",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CategoryProgressItem(category: String, amount: Double, percentage: Float, currency: String, color: Color) {
    val amountFormatted = String.format(Locale.getDefault(), "%,.2f", amount)
    val percentFormatted = (percentage * 100).toInt()
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = CategoryIcons.getIcon(category),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text(category, fontSize = 14.sp)
                Text("$currency $amountFormatted ($percentFormatted%)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { percentage.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = color,
                strokeCap = StrokeCap.Round
            )
        }
    }
}
