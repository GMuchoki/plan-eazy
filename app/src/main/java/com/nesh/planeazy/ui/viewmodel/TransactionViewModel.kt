package com.nesh.planeazy.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nesh.planeazy.data.local.AppDatabase
import com.nesh.planeazy.data.model.*
import com.nesh.planeazy.data.repository.TransactionRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository
    val allTransactions: StateFlow<List<Transaction>>
    val totalIncome: StateFlow<Double>
    val totalExpense: StateFlow<Double>
    val totalSavings: StateFlow<Double>
    val allBudgets: StateFlow<List<Budget>>
    val allGoals: StateFlow<List<Goal>>
    val allPaymentMethods: StateFlow<List<PaymentMethod>>

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private var isRestoring = false

    private val _syncMessage = MutableSharedFlow<String>()
    val syncMessage: SharedFlow<String> = _syncMessage

    init {
        val transactionDao = AppDatabase.getDatabase(application).transactionDao()
        repository = TransactionRepository(transactionDao)
        
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
    }

    suspend fun getTransactionById(id: Long): Transaction? {
        return repository.getTransactionById(id)
    }

    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                repository.insertTransaction(transaction)
                if (transaction.type == TransactionType.SAVINGS && transaction.goalId != null) {
                    repository.getGoalById(transaction.goalId)?.let { goal ->
                        val updatedGoal = goal.copy(savedAmount = goal.savedAmount + transaction.amount)
                        repository.insertGoal(updatedGoal)
                    }
                }
                if (!isRestoring) syncToCloud()
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Error adding transaction", e)
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                repository.deleteTransaction(transaction)
                if (transaction.type == TransactionType.SAVINGS && transaction.goalId != null) {
                    repository.getGoalById(transaction.goalId)?.let { goal ->
                        val updatedGoal = goal.copy(savedAmount = (goal.savedAmount - transaction.amount).coerceAtLeast(0.0))
                        repository.insertGoal(updatedGoal)
                    }
                }
                if (!isRestoring) syncToCloud()
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Error deleting transaction", e)
            }
        }
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

    fun addGoal(goal: Goal) {
        viewModelScope.launch {
            try {
                repository.insertGoal(goal)
                if (!isRestoring) syncToCloud()
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Error adding goal", e)
            }
        }
    }

    fun deleteGoal(goal: Goal) {
        viewModelScope.launch {
            try {
                repository.deleteGoal(goal)
                if (!isRestoring) syncToCloud()
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Error deleting goal", e)
            }
        }
    }

    fun addBudget(budget: Budget) {
        viewModelScope.launch {
            try {
                repository.insertBudget(budget)
                if (!isRestoring) syncToCloud()
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Error adding budget", e)
            }
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            try {
                repository.deleteBudget(budget)
                if (!isRestoring) syncToCloud()
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Error deleting budget", e)
            }
        }
    }

    // Cloud Sync Logic
    fun syncToCloud() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        if (isRestoring) return

        val db = FirebaseFirestore.getInstance()
        
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val transactions = repository.getAllTransactionsSync()
                val goals = repository.getAllGoalsSync()
                val budgets = repository.getAllBudgetsSync()

                if (transactions.isEmpty() && goals.isEmpty() && budgets.isEmpty()) {
                    _isSyncing.value = false
                    return@launch 
                }

                val userData = hashMapOf(
                    "transactions" to transactions.map { transactionToMap(it) },
                    "goals" to goals.map { goalToMap(it) },
                    "budgets" to budgets.map { budgetToMap(it) },
                    "lastSync" to System.currentTimeMillis()
                )

                db.collection("users").document(user.uid).set(userData).await()
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Sync failed", e)
                _syncMessage.emit("Cloud sync failed. Check your connection.")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun restoreFromCloud() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        
        viewModelScope.launch {
            isRestoring = true
            _isSyncing.value = true
            try {
                val document = db.collection("users").document(user.uid).get().await()
                if (document.exists()) {
                    val transactionsData = document.get("transactions") as? List<*>
                    val goalsData = document.get("goals") as? List<*>
                    
                    if (!transactionsData.isNullOrEmpty() || !goalsData.isNullOrEmpty()) {
                        repository.deleteAllData()
                        
                        transactionsData?.forEach { item ->
                            (item as? Map<String, Any>)?.let { repository.insertTransaction(mapToTransaction(it)) }
                        }

                        goalsData?.forEach { item ->
                            (item as? Map<String, Any>)?.let { repository.insertGoal(mapToGoal(it)) }
                        }
                        
                        (document.get("budgets") as? List<*>)?.forEach { item ->
                            (item as? Map<String, Any>)?.let { repository.insertBudget(mapToBudget(it)) }
                        }
                        _syncMessage.emit("Restore successful!")
                    }
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

    private fun transactionToMap(t: Transaction) = mapOf(
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
        "units" to t.units
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

    private fun mapToTransaction(map: Map<String, Any>): Transaction {
        return Transaction(
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
            units = (map["units"] as? Number)?.toDouble()
        )
    }

    private fun mapToGoal(map: Map<String, Any>): Goal {
        return Goal(
            title = map["title"] as? String ?: "",
            type = map["type"] as? String ?: "",
            targetAmount = (map["targetAmount"] as? Number)?.toDouble() ?: 0.0,
            savedAmount = (map["savedAmount"] as? Number)?.toDouble() ?: 0.0,
            deadline = (map["deadline"] as? Number)?.toLong(),
            status = map["status"] as? String ?: "Active",
            notes = map["notes"] as? String ?: ""
        )
    }

    private fun mapToBudget(map: Map<String, Any>): Budget {
        return Budget(
            category = map["category"] as? String ?: "",
            subCategory = map["subCategory"] as? String,
            amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
            month = (map["month"] as? Number)?.toInt() ?: 1,
            year = (map["year"] as? Number)?.toInt() ?: 2024
        )
    }

    fun seedSampleData() {
        viewModelScope.launch {
            try {
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
                goals.forEach { repository.insertGoal(it) }

                val sampleTransactions = listOf(
                    Transaction(amount = 60000.0, title = "Salary", date = System.currentTimeMillis(), category = "Salary", paymentMethodType = "Bank", paymentMethodProvider = "Equity Bank", note = "Jan Salary", type = TransactionType.INCOME),
                    Transaction(amount = 2500.0, title = "Groceries", date = System.currentTimeMillis(), category = "Food & Groceries", paymentMethodType = "Mobile Money", paymentMethodProvider = "M-Pesa", note = "Carrefour", type = TransactionType.EXPENSE)
                )
                sampleTransactions.forEach { repository.insertTransaction(it) }
                syncToCloud()
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Error seeding data", e)
            }
        }
    }
}
