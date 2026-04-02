package com.nesh.planeazy.util

import com.nesh.planeazy.data.model.Transaction
import com.nesh.planeazy.data.model.TransactionType
import java.util.Calendar

object TransactionUtils {

    fun calculateNextOccurrence(currentDate: Long, frequency: String): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = currentDate }
        when (frequency) {
            "Daily" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            "Weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            "Monthly" -> calendar.add(Calendar.MONTH, 1)
            "Yearly" -> calendar.add(Calendar.YEAR, 1)
        }
        return calendar.timeInMillis
    }

    fun calculateSpentForBudget(
        transactions: List<Transaction>,
        category: String,
        subCategory: String?,
        month: Int,
        year: Int
    ): Double {
        return transactions.filter {
            it.type == TransactionType.EXPENSE &&
            it.category == category &&
            (subCategory == null || it.subCategory == subCategory) &&
            isSameMonth(it.date, month, year)
        }.sumOf { it.amount }
    }

    fun isSameMonth(timestamp: Long, month: Int, year: Int): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return (cal.get(Calendar.MONTH) + 1) == month && cal.get(Calendar.YEAR) == year
    }
}
