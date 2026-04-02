package com.nesh.planeazy.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nesh.planeazy.data.local.AppDatabase
import com.nesh.planeazy.data.model.TransactionType
import java.util.Calendar

class BudgetWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.transactionDao()
        val preferenceManager = PreferenceManager(applicationContext)
        
        if (!preferenceManager.areNotificationsEnabled()) return Result.success()

        try {
            // 1. Check Budgets
            if (preferenceManager.areBudgetAlertsEnabled()) {
                val budgets = dao.getAllBudgetsSync()
                val allTransactions = dao.getAllTransactionsSync()
                
                val calendar = Calendar.getInstance()
                val currentMonth = calendar.get(Calendar.MONTH) + 1
                val currentYear = calendar.get(Calendar.YEAR)

                budgets.forEach { budget ->
                    if (budget.month == currentMonth && budget.year == currentYear) {
                        val spent = TransactionUtils.calculateSpentForBudget(
                            allTransactions,
                            budget.category,
                            budget.subCategory,
                            currentMonth,
                            currentYear
                        )

                        val progress = if (budget.amount > 0) spent / budget.amount else 0.0
                        
                        if (spent > budget.amount) {
                            NotificationHelper.showNotification(
                                applicationContext,
                                "Budget Exceeded!",
                                "You've gone over your budget for ${budget.subCategory ?: budget.category}",
                                budget.id.toInt()
                            )
                        } else if (progress >= 0.8) {
                            NotificationHelper.showNotification(
                                applicationContext,
                                "Budget Alert",
                                "You've used 80% of your budget for ${budget.subCategory ?: budget.category}",
                                budget.id.toInt()
                            )
                        }
                    }
                }
            }

            // 2. Check Goal Milestones
            if (preferenceManager.areGoalMilestonesEnabled()) {
                val goals = dao.getAllGoalsSync()
                goals.forEach { goal ->
                    val progress = if (goal.targetAmount > 0) goal.savedAmount / goal.targetAmount else 0.0
                    
                    when {
                        progress >= 1.0 -> {
                            NotificationHelper.showNotification(
                                applicationContext,
                                "Goal Achieved! 🎉",
                                "Congratulations! You've reached your target for '${goal.title}'!",
                                goal.id.toInt() + 1000
                            )
                        }
                        progress >= 0.9 -> {
                            NotificationHelper.showNotification(
                                applicationContext,
                                "Almost there! 🚀",
                                "You've reached 90% of your goal '${goal.title}'. Just a little more!",
                                goal.id.toInt() + 1000
                            )
                        }
                        progress >= 0.5 -> {
                            NotificationHelper.showNotification(
                                applicationContext,
                                "Halfway Point! 🏁",
                                "You've reached 50% of your goal '${goal.title}'! Great progress.",
                                goal.id.toInt() + 1000
                            )
                        }
                    }
                }
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e("BudgetWorker", "Error checking budgets/goals", e)
            return Result.failure()
        }
    }
}
