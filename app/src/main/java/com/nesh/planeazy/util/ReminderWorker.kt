package com.nesh.planeazy.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        NotificationHelper.showNotification(
            applicationContext,
            "Evening Reminder",
            "Don't forget to log your expenses for today! 📝",
            101
        )
        return Result.success()
    }
}
