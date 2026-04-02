package com.nesh.planeazy.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_categories")
data class UserCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val parentCategory: String? = null, // If null, it's a main category. If not null, it's a sub-category of this parent.
    val type: TransactionType = TransactionType.EXPENSE,
    val iconName: String? = null // Optional icon mapping
)
