package com.nesh.planeazy.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DebtType {
    OWED_TO_ME, // I lent money
    OWED_BY_ME  // I borrowed money
}

@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val personName: String,
    val totalAmount: Double,
    val paidAmount: Double = 0.0,
    val dueDate: Long? = null,
    val type: DebtType,
    val status: String = "Active", // "Active", "Settled"
    val notes: String = ""
)
