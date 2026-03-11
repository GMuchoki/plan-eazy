package com.example.plan_eazy.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.plan_eazy.data.local.AppDatabase
import com.example.plan_eazy.data.model.*
import com.example.plan_eazy.data.repository.TransactionRepository
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

    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.insertTransaction(transaction)
            if (transaction.type == TransactionType.SAVINGS && transaction.goalId != null) {
                repository.getGoalById(transaction.goalId)?.let { goal ->
                    val updatedGoal = goal.copy(savedAmount = goal.savedAmount + transaction.amount)
                    repository.insertGoal(updatedGoal)
                }
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            if (transaction.type == TransactionType.SAVINGS && transaction.goalId != null) {
                repository.getGoalById(transaction.goalId)?.let { goal ->
                    val updatedGoal = goal.copy(savedAmount = (goal.savedAmount - transaction.amount).coerceAtLeast(0.0))
                    repository.insertGoal(updatedGoal)
                }
            }
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.deleteAllData()
        }
    }

    fun addGoal(goal: Goal) {
        viewModelScope.launch {
            repository.insertGoal(goal)
        }
    }

    fun deleteGoal(goal: Goal) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
        }
    }

    fun addBudget(budget: Budget) {
        viewModelScope.launch {
            repository.insertBudget(budget)
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
        }
    }

    // Cloud Sync Logic
    fun syncToCloud() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val transactions = repository.allTransactions.first()
                val goals = repository.allGoals.first()
                val budgets = repository.allBudgets.first()

                val userData = hashMapOf(
                    "transactions" to transactions,
                    "goals" to goals,
                    "budgets" to budgets,
                    "lastSync" to System.currentTimeMillis()
                )

                db.collection("users").document(user.uid)
                    .set(userData)
                    .await()
                _syncMessage.emit("Backup successful!")
            } catch (e: Exception) {
                _syncMessage.emit("Backup failed: ${e.message}")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    // Cloud Restore Logic
    fun restoreFromCloud() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val document = db.collection("users").document(user.uid).get().await()
                if (document.exists()) {
                    repository.deleteAllData()
                    
                    val transactions = document.get("transactions") as? List<Map<String, Any>>
                    transactions?.forEach { map ->
                        repository.insertTransaction(mapToTransaction(map))
                    }

                    val goals = document.get("goals") as? List<Map<String, Any>>
                    goals?.forEach { map ->
                        repository.insertGoal(mapToGoal(map))
                    }
                    
                    val budgets = document.get("budgets") as? List<Map<String, Any>>
                    budgets?.forEach { map ->
                        repository.insertBudget(mapToBudget(map))
                    }
                    _syncMessage.emit("Restore successful!")
                } else {
                    _syncMessage.emit("No backup found.")
                }
            } catch (e: Exception) {
                _syncMessage.emit("Restore failed: ${e.message}")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private fun mapToTransaction(map: Map<String, Any>): Transaction {
        return Transaction(
            amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
            date = (map["date"] as? Number)?.toLong() ?: 0,
            title = map["title"] as? String ?: "",
            category = map["category"] as? String ?: "",
            paymentMethodType = map["paymentMethodType"] as? String ?: "",
            paymentMethodProvider = map["paymentMethodProvider"] as? String ?: "",
            note = map["note"] as? String ?: "",
            type = TransactionType.valueOf(map["type"] as? String ?: "EXPENSE"),
            goalId = (map["goalId"] as? Number)?.toLong()
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
            amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
            month = (map["month"] as? Number)?.toInt() ?: 1,
            year = (map["year"] as? Number)?.toInt() ?: 2024
        )
    }

    fun seedSampleData() {
        viewModelScope.launch {
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
        }
    }
}
