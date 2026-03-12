package com.nesh.planeazy.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val type: String = "",
    val targetAmount: Double = 0.0,
    val savedAmount: Double = 0.0,
    val deadline: Long? = null,
    val status: String = "Active",
    val notes: String = ""
)
