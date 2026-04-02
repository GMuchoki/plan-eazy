package com.nesh.planeazy.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nesh.planeazy.data.local.AppDatabase
import com.nesh.planeazy.data.model.Transaction
import java.util.Calendar

class RecurringTransactionWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.transactionDao()
        
        try {
            val templates = dao.getAllTemplatesSync()
            val now = System.currentTimeMillis()
            
            templates.forEach { template ->
                var nextOcc = template.nextOccurrence ?: return@forEach
                
                while (nextOcc <= now) {
                    // 1. Create a new real transaction from the template
                    val newTransaction = template.copy(
                        id = 0,
                        date = nextOcc,
                        isTemplate = false,
                        nextOccurrence = null
                    )
                    dao.insertTransaction(newTransaction)
                    
                    // 2. Update the template's nextOccurrence
                    val frequency = template.frequency ?: "Monthly"
                    nextOcc = TransactionUtils.calculateNextOccurrence(nextOcc, frequency)
                    
                    val updatedTemplate = template.copy(nextOccurrence = nextOcc)
                    dao.insertTransaction(updatedTemplate)
                    
                    Log.d("RecurringWorker", "Generated recurring transaction: ${template.title} for date $nextOcc")
                }
            }
            return Result.success()
        } catch (e: Exception) {
            Log.e("RecurringWorker", "Error processing recurring transactions", e)
            return Result.failure()
        }
    }
}
