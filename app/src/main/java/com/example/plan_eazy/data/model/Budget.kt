package com.example.plan_eazy.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String,
    val subCategory: String? = null,
    val amount: Double,
    val month: Int, // 1-12
    val year: Int
)
