package com.example.plan_eazy.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    INCOME, EXPENSE, SAVINGS
}

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double = 0.0,
    val date: Long = 0,
    val title: String = "",
    val category: String = "",
    val paymentMethodType: String = "",
    val paymentMethodProvider: String = "",
    val note: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val goalId: Long? = null
)
