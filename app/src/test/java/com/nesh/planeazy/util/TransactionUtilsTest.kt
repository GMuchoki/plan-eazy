package com.nesh.planeazy.util

import com.nesh.planeazy.data.model.Transaction
import com.nesh.planeazy.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class TransactionUtilsTest {

    @Test
    fun testCalculateNextOccurrence_Daily() {
        val calendar = Calendar.getInstance().apply {
            set(2024, Calendar.JANUARY, 1, 10, 0)
        }
        val current = calendar.timeInMillis
        
        val next = TransactionUtils.calculateNextOccurrence(current, "Daily")
        
        val nextCalendar = Calendar.getInstance().apply { timeInMillis = next }
        assertEquals(2024, nextCalendar.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, nextCalendar.get(Calendar.MONTH))
        assertEquals(2, nextCalendar.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testCalculateNextOccurrence_Monthly() {
        val calendar = Calendar.getInstance().apply {
            set(2024, Calendar.JANUARY, 31, 10, 0)
        }
        val current = calendar.timeInMillis
        
        val next = TransactionUtils.calculateNextOccurrence(current, "Monthly")
        
        val nextCalendar = Calendar.getInstance().apply { timeInMillis = next }
        assertEquals(2024, nextCalendar.get(Calendar.YEAR))
        assertEquals(Calendar.FEBRUARY, nextCalendar.get(Calendar.MONTH))
        // February 2024 has 29 days
        assertEquals(29, nextCalendar.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testIsSameMonth_True() {
        val calendar = Calendar.getInstance().apply {
            set(2024, Calendar.MARCH, 15)
        }
        assertTrue(TransactionUtils.isSameMonth(calendar.timeInMillis, 3, 2024))
    }

    @Test
    fun testCalculateSpentForBudget_CorrectFiltering() {
        val transactions = listOf(
            Transaction(amount = 100.0, category = "Food", type = TransactionType.EXPENSE, date = createDate(2024, Calendar.JANUARY, 10)),
            Transaction(amount = 50.0, category = "Food", type = TransactionType.EXPENSE, date = createDate(2024, Calendar.JANUARY, 20)),
            Transaction(amount = 200.0, category = "Rent", type = TransactionType.EXPENSE, date = createDate(2024, Calendar.JANUARY, 5)),
            Transaction(amount = 500.0, category = "Food", type = TransactionType.INCOME, date = createDate(2024, Calendar.JANUARY, 15)),
            Transaction(amount = 30.0, category = "Food", type = TransactionType.EXPENSE, date = createDate(2024, Calendar.FEBRUARY, 1))
        )

        val spent = TransactionUtils.calculateSpentForBudget(transactions, "Food", null, 1, 2024)
        
        assertEquals(150.0, spent, 0.01)
    }

    private fun createDate(year: Int, month: Int, day: Int): Long {
        return Calendar.getInstance().apply {
            set(year, month, day)
        }.timeInMillis
    }
}
