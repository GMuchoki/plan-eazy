package com.example.plan_eazy

import android.app.Application
import com.google.firebase.FirebaseApp

class PlanEazyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Firebase is automatically initialized by the ContentProvider if google-services.json is present
        // but explicit initialization doesn't hurt.
        FirebaseApp.initializeApp(this)
    }
}
