package com.nesh.planeazy.data.model

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
    val subCategory: String? = null,
    val paymentMethodType: String = "",
    val paymentMethodProvider: String = "",
    val note: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val goalId: Long? = null,
    val units: Double? = null,
    
    // Recurring Transaction fields
    val isRecurring: Boolean = false,
    val frequency: String? = null, // "Daily", "Weekly", "Monthly", "Yearly"
    val nextOccurrence: Long? = null,
    val isTemplate: Boolean = false, // If true, this is the master record used to generate others
    
    // Attachment support
    val attachmentUri: String? = null
)
