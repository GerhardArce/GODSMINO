package com.example.godsmino

import android.app.Application
import com.google.firebase.FirebaseApp

class GodsminoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
        } catch (_: Exception) {}
    }
}
