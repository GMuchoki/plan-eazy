package com.nesh.planeazy.data.repository

import com.nesh.planeazy.data.local.TransactionDao
import com.nesh.planeazy.data.model.*
import com.nesh.planeazy.data.model.Transaction as AppTransaction
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(private val transactionDao: TransactionDao) {

    val allTransactions: Flow<List<AppTransaction>> = transactionDao.getAllTransactions()
    val allBudgets: Flow<List<Budget>> = transactionDao.getAllBudgets()
    val allGoals: Flow<List<Goal>> = transactionDao.getAllGoals()
    val allPaymentMethods: Flow<List<PaymentMethod>> = transactionDao.getAllPaymentMethods()
    val allDebts: Flow<List<Debt>> = transactionDao.getAllDebts()
    val allUserCategories: Flow<List<UserCategory>> = transactionDao.getAllUserCategories()

    suspend fun insertTransaction(transaction: AppTransaction): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: AppTransaction) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun getTransactionById(id: Long): AppTransaction? {
        return transactionDao.getTransactionById(id)
    }

    suspend fun deleteAllData() {
        transactionDao.deleteAllTransactions()
        transactionDao.deleteAllBudgets()
        transactionDao.deleteAllGoals()
        transactionDao.deleteAllPaymentMethods()
        transactionDao.deleteAllDebts()
        transactionDao.deleteAllUserCategories()
    }

    // Goals
    suspend fun insertGoal(goal: Goal): Long {
        return transactionDao.insertGoal(goal)
    }

    suspend fun deleteGoal(goal: Goal) {
        transactionDao.deleteGoal(goal)
    }

    suspend fun getGoalById(id: Long): Goal? {
        return transactionDao.getGoalById(id)
    }

    // Payment Methods
    suspend fun insertPaymentMethod(paymentMethod: PaymentMethod): Long {
        return transactionDao.insertPaymentMethod(paymentMethod)
    }

    suspend fun deletePaymentMethod(paymentMethod: PaymentMethod) {
        transactionDao.deletePaymentMethod(paymentMethod)
    }

    // Budget
    suspend fun insertBudget(budget: Budget): Long {
        return transactionDao.insertBudget(budget)
    }

    suspend fun deleteBudget(budget: Budget) {
        transactionDao.deleteBudget(budget)
    }

    fun getTotalByType(type: TransactionType): Flow<Double?> {
        return transactionDao.getTotalByType(type)
    }

    // Debts
    suspend fun insertDebt(debt: Debt): Long {
        return transactionDao.insertDebt(debt)
    }

    suspend fun deleteDebt(debt: Debt) {
        transactionDao.deleteDebt(debt)
    }

    suspend fun getAllDebtsSync() = transactionDao.getAllDebtsSync()

    // User Categories
    suspend fun insertUserCategory(category: UserCategory): Long {
        return transactionDao.insertUserCategory(category)
    }

    suspend fun deleteUserCategory(category: UserCategory) {
        transactionDao.deleteUserCategory(category)
    }

    suspend fun getAllUserCategoriesSync() = transactionDao.getAllUserCategoriesSync()

    // Usage check helpers
    suspend fun getTransactionCountForCategory(category: String) = transactionDao.getTransactionCountForCategory(category)
    suspend fun getBudgetCountForCategory(category: String) = transactionDao.getBudgetCountForCategory(category)
    suspend fun getGoalCountForCategory(category: String) = transactionDao.getGoalCountForCategory(category)

    // Reassignment helpers
    suspend fun reassignTransactionCategory(oldCategory: String, newCategory: String) = transactionDao.reassignTransactionCategory(oldCategory, newCategory)
    suspend fun reassignTransactionSubCategory(oldSub: String, newSub: String?) = transactionDao.reassignTransactionSubCategory(oldSub, newSub)
    suspend fun reassignBudgetCategory(oldCategory: String, newCategory: String) = transactionDao.reassignBudgetCategory(oldCategory, newCategory)
    suspend fun reassignBudgetSubCategory(oldSub: String, newSub: String?) = transactionDao.reassignBudgetSubCategory(oldSub, newSub)
    suspend fun reassignGoalType(oldType: String, newType: String) = transactionDao.reassignGoalType(oldType, newType)

    // Sync helpers
    suspend fun getAllTransactionsSync() = transactionDao.getAllTransactionsSync()
    suspend fun getAllTemplatesSync() = transactionDao.getAllTemplatesSync()
    suspend fun getAllGoalsSync() = transactionDao.getAllGoalsSync()
    suspend fun getAllBudgetsSync() = transactionDao.getAllBudgetsSync()
}
