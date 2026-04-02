package com.nesh.planeazy.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nesh.planeazy.data.model.*
import com.nesh.planeazy.data.model.Transaction as AppTransaction
import com.nesh.planeazy.data.repository.TransactionRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : ViewModel() {

    val allTransactions: StateFlow<List<AppTransaction>>
    val totalIncome: StateFlow<Double>
    val totalExpense: StateFlow<Double>
    val totalSavings: StateFlow<Double>
    val allBudgets: StateFlow<List<Budget>>
    val allGoals: StateFlow<List<Goal>>
    val allPaymentMethods: StateFlow<List<PaymentMethod>>
    val allDebts: StateFlow<List<Debt>>
    val allUserCategories: StateFlow<List<UserCategory>>

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private var isRestoring = false

    private val _syncMessage = MutableSharedFlow<String>()
    val syncMessage: SharedFlow<String> = _syncMessage

    init {
        allTransactions = repository.allTransactions
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
            
        totalIncome = repository.getTotalByType(TransactionType.INCOME)
            .map { it ?: 0.0 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
            
        totalExpense = repository.getTotalByType(TransactionType.EXPENSE)
            .map { it ?: 0.0 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
            
        totalSavings = repository.getTotalByType(TransactionType.SAVINGS)
            .map { it ?: 0.0 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        allBudgets = repository.allBudgets
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allGoals = repository.allGoals
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allPaymentMethods = repository.allPaymentMethods
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allDebts = repository.allDebts
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allUserCategories = repository.allUserCategories
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    suspend fun getTransactionById(id: Long): AppTransaction? {
        return repository.getTransactionById(id)
    }

    fun addTransaction(transaction: AppTransaction) {
        viewModelScope.launch {
            try {
                val id = repository.insertTransaction(transaction)
                val insertedTransaction = transaction.copy(id = id)
                
                if (!transaction.isTemplate) {
                    if (transaction.type == TransactionType.SAVINGS && transaction.goalId != null) {
                        repository.getGoalById(transaction.goalId)?.let { goal ->
                            val updatedGoal = goal.copy(savedAmount = goal.savedAmount + transaction.amount)
                            repository.insertGoal(updatedGoal)
                            if (!isRestoring) uploadGoal(updatedGoal)
                        }
                    }
                }
                if (!isRestoring) uploadTransaction(insertedTransaction)
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Error adding transaction", e)
            }
        }
    }

    fun deleteTransaction(transaction: AppTransaction) {
        viewModelScope.launch {
            try {
                repository.deleteTransaction(transaction)
                if (!transaction.isTemplate) {
                    if (transaction.type == TransactionType.SAVINGS && transaction.goalId != null) {
                        repository.getGoalById(transaction.goalId)?.let { goal ->
                            val updatedGoal = goal.copy(savedAmount = (goal.savedAmount - transaction.amount).coerceAtLeast(0.0))
                            repository.insertGoal(updatedGoal)
                            if (!isRestoring) uploadGoal(updatedGoal)
                        }
                    }
                }
                if (!isRestoring) removeFromCloud("transactions", transaction.id.toString())
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Error deleting transaction", e)
            }
        }
    }

    fun deleteTransactions(transactions: List<AppTransaction>) {
        viewModelScope.launch {
            transactions.forEach { deleteTransaction(it) }
        }
    }

    fun addGoal(goal: Goal) {
        viewModelScope.launch {
            try {
                val id = repository.insertGoal(goal)
                if (!isRestoring) uploadGoal(goal.copy(id = id))
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Error adding goal", e)
            }
        }
    }

    fun deleteGoal(goal: Goal) {
        viewModelScope.launch {
            try {
                repository.deleteGoal(goal)
                if (!isRestoring) removeFromCloud("goals", goal.id.toString())
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Error deleting goal", e)
            }
        }
    }

    fun addBudget(budget: Budget) {
        viewModelScope.launch {
            try {
                val id = repository.insertBudget(budget)
                if (!isRestoring) uploadBudget(budget.copy(id = id))
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Error adding budget", e)
            }
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            try {
                repository.deleteBudget(budget)
                if (!isRestoring) removeFromCloud("budgets", budget.id.toString())
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Error deleting budget", e)
            }
        }
    }

    fun addDebt(debt: Debt) {
        viewModelScope.launch {
            try {
                val id = repository.insertDebt(debt)
                if (!isRestoring) uploadDebt(debt.copy(id = id))
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Error adding debt", e)
            }
        }
    }

    fun deleteDebt(debt: Debt) {
        viewModelScope.launch {
            try {
                repository.deleteDebt(debt)
                if (!isRestoring) removeFromCloud("debts", debt.id.toString())
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Error deleting debt", e)
            }
        }
    }

    fun addUserCategory(category: UserCategory) {
        viewModelScope.launch {
            try {
                val id = repository.insertUserCategory(category)
                if (!isRestoring) uploadUserCategory(category.copy(id = id))
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Error adding category", e)
            }
        }
    }

    fun deleteUserCategory(category: UserCategory) {
        viewModelScope.launch {
            try {
                repository.deleteUserCategory(category)
                if (!isRestoring) removeFromCloud("user_categories", category.id.toString())
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Error deleting category", e)
            }
        }
    }

    suspend fun getTransactionCountForCategory(name: String): Int {
        return repository.getTransactionCountForCategory(name)
    }

    suspend fun getBudgetCountForCategory(name: String): Int {
        return repository.getBudgetCountForCategory(name)
    }

    fun deleteUserCategoryWithReassignment(category: UserCategory) {
        viewModelScope.launch {
            try {
                val fallback = "Other"
                
                if (category.parentCategory == null) {
                    repository.reassignTransactionCategory(category.name, fallback)
                    repository.reassignBudgetCategory(category.name, fallback)
                    repository.reassignGoalType(category.name, fallback)
                } else {
                    repository.reassignTransactionSubCategory(category.name, null)
                    repository.reassignBudgetSubCategory(category.name, null)
                }
                
                repository.deleteUserCategory(category)
                if (!isRestoring) {
                    removeFromCloud("user_categories", category.id.toString())
                    syncToCloud()
                }
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Error deleting category with reassignment", e)
            }
        }
    }

    // Individual Upload Helpers
    private fun uploadTransaction(t: AppTransaction) {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid)
            .collection("transactions").document(t.id.toString())
            .set(transactionToMap(t))
    }

    private fun uploadGoal(g: Goal) {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid)
            .collection("goals").document(g.id.toString())
            .set(goalToMap(g))
    }

    private fun uploadBudget(b: Budget) {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid)
            .collection("budgets").document(b.id.toString())
            .set(budgetToMap(b))
    }

    private fun uploadDebt(d: Debt) {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid)
            .collection("debts").document(d.id.toString())
            .set(debtToMap(d))
    }

    private fun uploadUserCategory(c: UserCategory) {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid)
            .collection("user_categories").document(c.id.toString())
            .set(userCategoryToMap(c))
    }

    private fun removeFromCloud(collection: String, docId: String) {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid)
            .collection(collection).document(docId)
            .delete()
    }

    fun syncToCloud() {
        val user = auth.currentUser ?: return
        if (isRestoring) return

        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val transactions = repository.getAllTransactionsSync() + repository.getAllTemplatesSync()
                val goals = repository.getAllGoalsSync()
                val budgets = repository.getAllBudgetsSync()
                val debts = repository.getAllDebtsSync()
                val categories = repository.getAllUserCategoriesSync()

                val userRef = db.collection("users").document(user.uid)
                
                val batch = db.batch()
                
                transactions.forEach { 
                    batch.set(userRef.collection("transactions").document(it.id.toString()), transactionToMap(it))
                }
                goals.forEach { 
                    batch.set(userRef.collection("goals").document(it.id.toString()), goalToMap(it))
                }
                budgets.forEach { 
                    batch.set(userRef.collection("budgets").document(it.id.toString()), budgetToMap(it))
                }
                debts.forEach { 
                    batch.set(userRef.collection("debts").document(it.id.toString()), debtToMap(it))
                }
                categories.forEach { 
                    batch.set(userRef.collection("user_categories").document(it.id.toString()), userCategoryToMap(it))
                }
                
                batch.set(userRef, mapOf("lastSync" to System.currentTimeMillis()), SetOptions.merge())
                batch.commit().await()
                
                _syncMessage.emit("Cloud sync complete")
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Sync failed", e)
                _syncMessage.emit("Sync failed: ${e.message}")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun restoreFromCloud() {
        val user = auth.currentUser ?: return
        
        viewModelScope.launch {
            isRestoring = true
            _isSyncing.value = true
            try {
                val userRef = db.collection("users").document(user.uid)
                
                val transSnapshot = userRef.collection("transactions").get().await()
                val goalsSnapshot = userRef.collection("goals").get().await()
                val budgetsSnapshot = userRef.collection("budgets").get().await()
                val debtsSnapshot = userRef.collection("debts").get().await()
                val catSnapshot = userRef.collection("user_categories").get().await()

                if (!transSnapshot.isEmpty || !goalsSnapshot.isEmpty || !budgetsSnapshot.isEmpty || !debtsSnapshot.isEmpty || !catSnapshot.isEmpty) {
                    repository.deleteAllData()
                    
                    transSnapshot.documents.forEach { doc ->
                        doc.data?.let { repository.insertTransaction(mapToTransaction(it, doc.id.toLong())) }
                    }

                    goalsSnapshot.documents.forEach { doc ->
                        doc.data?.let { repository.insertGoal(mapToGoal(it, doc.id.toLong())) }
                    }
                    
                    budgetsSnapshot.documents.forEach { doc ->
                        doc.data?.let { repository.insertBudget(mapToBudget(it, doc.id.toLong())) }
                    }

                    debtsSnapshot.documents.forEach { doc ->
                        doc.data?.let { repository.insertDebt(mapToDebt(it, doc.id.toLong())) }
                    }

                    catSnapshot.documents.forEach { doc ->
                        doc.data?.let { repository.insertUserCategory(mapToUserCategory(it, doc.id.toLong())) }
                    }
                    _syncMessage.emit("Data restored from cloud!")
                } else {
                    _syncMessage.emit("No cloud profile found.")
                }
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Restore failed", e)
                _syncMessage.emit("Restore failed: ${e.message}")
            } finally {
                _isSyncing.value = false
                isRestoring = false
            }
        }
    }

    private fun transactionToMap(t: AppTransaction) = mapOf(
        "amount" to t.amount,
        "date" to t.date,
        "title" to t.title,
        "category" to t.category,
        "subCategory" to t.subCategory,
        "paymentMethodType" to t.paymentMethodType,
        "paymentMethodProvider" to t.paymentMethodProvider,
        "note" to t.note,
        "type" to t.type.name,
        "goalId" to t.goalId,
        "units" to t.units,
        "isRecurring" to t.isRecurring,
        "frequency" to t.frequency,
        "nextOccurrence" to t.nextOccurrence,
        "isTemplate" to t.isTemplate,
        "attachmentUri" to t.attachmentUri
    )

    private fun goalToMap(g: Goal) = mapOf(
        "title" to g.title,
        "type" to g.type,
        "targetAmount" to g.targetAmount,
        "savedAmount" to g.savedAmount,
        "deadline" to g.deadline,
        "status" to g.status,
        "notes" to g.notes
    )

    private fun budgetToMap(b: Budget) = mapOf(
        "category" to b.category,
        "subCategory" to b.subCategory,
        "amount" to b.amount,
        "month" to b.month,
        "year" to b.year
    )

    private fun debtToMap(d: Debt) = mapOf(
        "title" to d.title,
        "personName" to d.personName,
        "totalAmount" to d.totalAmount,
        "paidAmount" to d.paidAmount,
        "dueDate" to d.dueDate,
        "type" to d.type.name,
        "status" to d.status,
        "notes" to d.notes
    )

    private fun userCategoryToMap(c: UserCategory) = mapOf(
        "name" to c.name,
        "parentCategory" to c.parentCategory,
        "type" to c.type.name,
        "iconName" to c.iconName
    )

    private fun mapToTransaction(map: Map<String, Any>, id: Long): AppTransaction {
        return AppTransaction(
            id = id,
            amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
            date = (map["date"] as? Number)?.toLong() ?: 0,
            title = map["title"] as? String ?: "",
            category = map["category"] as? String ?: "",
            subCategory = map["subCategory"] as? String,
            paymentMethodType = map["paymentMethodType"] as? String ?: "",
            paymentMethodProvider = map["paymentMethodProvider"] as? String ?: "",
            note = map["note"] as? String ?: "",
            type = TransactionType.valueOf(map["type"] as? String ?: "EXPENSE"),
            goalId = (map["goalId"] as? Number)?.toLong(),
            units = (map["units"] as? Number)?.toDouble(),
            isRecurring = map["isRecurring"] as? Boolean ?: false,
            frequency = map["frequency"] as? String,
            nextOccurrence = (map["nextOccurrence"] as? Number)?.toLong(),
            isTemplate = map["isTemplate"] as? Boolean ?: false,
            attachmentUri = map["attachmentUri"] as? String
        )
    }

    private fun mapToGoal(map: Map<String, Any>, id: Long): Goal {
        return Goal(
            id = id,
            title = map["title"] as? String ?: "",
            type = map["type"] as? String ?: "",
            targetAmount = (map["targetAmount"] as? Number)?.toDouble() ?: 0.0,
            savedAmount = (map["savedAmount"] as? Number)?.toDouble() ?: 0.0,
            deadline = (map["deadline"] as? Number)?.toLong(),
            status = map["status"] as? String ?: "Active",
            notes = map["notes"] as? String ?: ""
        )
    }

    private fun mapToBudget(map: Map<String, Any>, id: Long): Budget {
        return Budget(
            id = id,
            category = map["category"] as? String ?: "",
            subCategory = map["subCategory"] as? String,
            amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
            month = (map["month"] as? Number)?.toInt() ?: 1,
            year = (map["year"] as? Number)?.toInt() ?: 2024
        )
    }

    private fun mapToDebt(map: Map<String, Any>, id: Long): Debt {
        return Debt(
            id = id,
            title = map["title"] as? String ?: "",
            personName = map["personName"] as? String ?: "",
            totalAmount = (map["totalAmount"] as? Number)?.toDouble() ?: 0.0,
            paidAmount = (map["paidAmount"] as? Number)?.toDouble() ?: 0.0,
            dueDate = (map["dueDate"] as? Number)?.toLong(),
            type = DebtType.valueOf(map["type"] as? String ?: "OWED_BY_ME"),
            status = map["status"] as? String ?: "Active",
            notes = map["notes"] as? String ?: ""
        )
    }

    private fun mapToUserCategory(map: Map<String, Any>, id: Long): UserCategory {
        return UserCategory(
            id = id,
            name = map["name"] as? String ?: "",
            parentCategory = map["parentCategory"] as? String,
            type = TransactionType.valueOf(map["type"] as? String ?: "EXPENSE"),
            iconName = map["iconName"] as? String
        )
    }

    fun resetAllData() {
        viewModelScope.launch {
            try {
                repository.deleteAllData()
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Error resetting data", e)
            }
        }
    }

    fun seedSampleData(onFailure: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val existing = repository.getAllTransactionsSync()
                if (existing.isNotEmpty()) {
                    onFailure("Cannot seed sample data while real transactions exist. Please reset data first.")
                    return@launch
                }

                val methods = listOf(
                    PaymentMethod(type = "Mobile Money", provider = "M-Pesa"),
                    PaymentMethod(type = "Bank", provider = "Equity Bank"),
                    PaymentMethod(type = "Cash", provider = "Wallet")
                )
                methods.forEach { repository.insertPaymentMethod(it) }

                val goals = listOf(
                    Goal(title = "Emergency Fund", type = "Emergency Fund", targetAmount = 100000.0, savedAmount = 15000.0),
                    Goal(title = "New Laptop", type = "Custom", targetAmount = 80000.0, savedAmount = 5000.0)
                )
                goals.forEach { goalsItem -> 
                    val id = repository.insertGoal(goalsItem)
                    uploadGoal(goalsItem.copy(id = id))
                }

                val sampleTransactions = listOf(
                    AppTransaction(amount = 60000.0, title = "Salary", date = System.currentTimeMillis(), category = "Salary", paymentMethodType = "Bank", paymentMethodProvider = "Equity Bank", note = "Jan Salary", type = TransactionType.INCOME),
                    AppTransaction(amount = 2500.0, title = "Groceries", date = System.currentTimeMillis(), category = "Food & Groceries", paymentMethodType = "Mobile Money", paymentMethodProvider = "M-Pesa", note = "Carrefour", type = TransactionType.EXPENSE)
                )
                sampleTransactions.forEach { trans ->
                    val id = repository.insertTransaction(trans)
                    uploadTransaction(trans.copy(id = id))
                }
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Error seeding data", e)
                onFailure("Failed to seed data: ${e.message}")
            }
        }
    }
}
