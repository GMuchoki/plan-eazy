package com.nesh.planeazy.data.local

import androidx.room.*
import com.nesh.planeazy.data.model.Budget
import com.nesh.planeazy.data.model.Transaction as AppTransaction
import com.nesh.planeazy.data.model.TransactionType
import com.nesh.planeazy.data.model.Goal
import com.nesh.planeazy.data.model.PaymentMethod
import com.nesh.planeazy.data.model.Debt
import com.nesh.planeazy.data.model.UserCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE isTemplate = 0 ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<AppTransaction>>

    @Query("SELECT * FROM transactions WHERE isTemplate = 0")
    suspend fun getAllTransactionsSync(): List<AppTransaction>

    @Query("SELECT * FROM transactions WHERE isTemplate = 1")
    suspend fun getAllTemplatesSync(): List<AppTransaction>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): AppTransaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: AppTransaction): Long

    @Delete
    suspend fun deleteTransaction(transaction: AppTransaction)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type AND isTemplate = 0")
    fun getTotalByType(type: TransactionType): Flow<Double?>

    // Usage check helpers
    @Query("SELECT COUNT(*) FROM transactions WHERE category = :category OR subCategory = :category")
    suspend fun getTransactionCountForCategory(category: String): Int

    @Query("SELECT COUNT(*) FROM budgets WHERE category = :category OR subCategory = :category")
    suspend fun getBudgetCountForCategory(category: String): Int

    @Query("SELECT COUNT(*) FROM goals WHERE type = :category")
    suspend fun getGoalCountForCategory(category: String): Int

    // Reassignment helpers
    @Query("UPDATE transactions SET category = :newCategory WHERE category = :oldCategory")
    suspend fun reassignTransactionCategory(oldCategory: String, newCategory: String)

    @Query("UPDATE transactions SET subCategory = :newSub WHERE subCategory = :oldSub")
    suspend fun reassignTransactionSubCategory(oldSub: String, newSub: String?)

    @Query("UPDATE budgets SET category = :newCategory WHERE category = :oldCategory")
    suspend fun reassignBudgetCategory(oldCategory: String, newCategory: String)

    @Query("UPDATE budgets SET subCategory = :newSub WHERE subCategory = :oldSub")
    suspend fun reassignBudgetSubCategory(oldSub: String, newSub: String?)

    @Query("UPDATE goals SET type = :newType WHERE type = :oldType")
    suspend fun reassignGoalType(oldType: String, newType: String)

    // Goals
    @Query("SELECT * FROM goals")
    fun getAllGoals(): Flow<List<Goal>>

    @Query("SELECT * FROM goals")
    suspend fun getAllGoalsSync(): List<Goal>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getGoalById(id: Long): Goal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal): Long

    @Delete
    suspend fun deleteGoal(goal: Goal)

    @Query("DELETE FROM goals")
    suspend fun deleteAllGoals()

    // Payment Methods
    @Query("SELECT * FROM payment_methods")
    fun getAllPaymentMethods(): Flow<List<PaymentMethod>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentMethod(paymentMethod: PaymentMethod): Long

    @Delete
    suspend fun deletePaymentMethod(paymentMethod: PaymentMethod)

    @Query("DELETE FROM payment_methods")
    suspend fun deleteAllPaymentMethods()

    // Budget
    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<Budget>>

    @Query("SELECT * FROM budgets")
    suspend fun getAllBudgetsSync(): List<Budget>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget): Long

    @Delete
    suspend fun deleteBudget(budget: Budget)

    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets()

    // Debts
    @Query("SELECT * FROM debts ORDER BY dueDate ASC")
    fun getAllDebts(): Flow<List<Debt>>

    @Query("SELECT * FROM debts")
    suspend fun getAllDebtsSync(): List<Debt>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: Debt): Long

    @Delete
    suspend fun deleteDebt(debt: Debt)

    @Query("DELETE FROM debts")
    suspend fun deleteAllDebts()

    // User Categories
    @Query("SELECT * FROM user_categories")
    fun getAllUserCategories(): Flow<List<UserCategory>>

    @Query("SELECT * FROM user_categories")
    suspend fun getAllUserCategoriesSync(): List<UserCategory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserCategory(category: UserCategory): Long

    @Delete
    suspend fun deleteUserCategory(category: UserCategory)

    @Query("DELETE FROM user_categories")
    suspend fun deleteAllUserCategories()
}
