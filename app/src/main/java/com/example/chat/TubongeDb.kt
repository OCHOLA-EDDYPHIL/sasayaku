package com.example.chat

import android.app.Application
import com.google.firebase.database.FirebaseDatabase

class TubongeDb : Application() {
    override fun onCreate() {
        super.onCreate()
        // Enable Firebase offline capabilities
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }
}