package com.example.plan_eazy.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payment_methods")
data class PaymentMethod(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // Cash, Mobile Money, Bank, Card, Online Wallet, Other
    val provider: String, // M-Pesa, Equity, etc.
    val isCustom: Boolean = false
)
