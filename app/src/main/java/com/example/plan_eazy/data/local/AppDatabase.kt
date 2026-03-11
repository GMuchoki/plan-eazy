package com.example.plan_eazy.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.plan_eazy.data.model.Budget
import com.example.plan_eazy.data.model.Transaction
import com.example.plan_eazy.data.model.Goal
import com.example.plan_eazy.data.model.PaymentMethod

@Database(
    entities = [Transaction::class, Budget::class, Goal::class, PaymentMethod::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "plan_eazy_db"
                )
                .fallbackToDestructiveMigration() // For development, wipe DB on schema change
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
