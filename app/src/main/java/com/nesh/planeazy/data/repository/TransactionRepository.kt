package com.nesh.planeazy.data.repository

import com.nesh.planeazy.data.local.TransactionDao
import com.nesh.planeazy.data.model.*
import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val transactionDao: TransactionDao) {

    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val allBudgets: Flow<List<Budget>> = transactionDao.getAllBudgets()
    val allGoals: Flow<List<Goal>> = transactionDao.getAllGoals()
    val allPaymentMethods: Flow<List<PaymentMethod>> = transactionDao.getAllPaymentMethods()

    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteAllData() {
        transactionDao.deleteAllTransactions()
        transactionDao.deleteAllBudgets()
        transactionDao.deleteAllGoals()
        transactionDao.deleteAllPaymentMethods()
    }

    // Goals
    suspend fun insertGoal(goal: Goal) {
        transactionDao.insertGoal(goal)
    }

    suspend fun deleteGoal(goal: Goal) {
        transactionDao.deleteGoal(goal)
    }

    suspend fun getGoalById(id: Long): Goal? {
        return transactionDao.getGoalById(id)
    }

    // Payment Methods
    suspend fun insertPaymentMethod(paymentMethod: PaymentMethod) {
        transactionDao.insertPaymentMethod(paymentMethod)
    }

    suspend fun deletePaymentMethod(paymentMethod: PaymentMethod) {
        transactionDao.deletePaymentMethod(paymentMethod)
    }

    // Budget
    suspend fun insertBudget(budget: Budget) {
        transactionDao.insertBudget(budget)
    }

    suspend fun deleteBudget(budget: Budget) {
        transactionDao.deleteBudget(budget)
    }

    fun getTotalByType(type: TransactionType): Flow<Double?> {
        return transactionDao.getTotalByType(type)
    }

    // Sync helpers
    suspend fun getAllTransactionsSync() = transactionDao.getAllTransactionsSync()
    suspend fun getAllGoalsSync() = transactionDao.getAllGoalsSync()
    suspend fun getAllBudgetsSync() = transactionDao.getAllBudgetsSync()
}
