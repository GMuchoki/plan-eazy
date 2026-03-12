package com.nesh.planeazy

import android.app.Application
import com.nesh.planeazy.util.NotificationHelper
import com.google.firebase.FirebaseApp

class PlanEazyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        NotificationHelper.createNotificationChannel(this)
    }
}
