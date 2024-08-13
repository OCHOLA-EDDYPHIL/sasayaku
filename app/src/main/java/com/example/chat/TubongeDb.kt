package com.example.chat

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class TubongeDb : Application() {

    companion object {
        private lateinit var auth: FirebaseAuth
        private lateinit var database: FirebaseDatabase

        fun getAuth(): FirebaseAuth = auth
        fun getDatabase(): FirebaseDatabase = database
    }

    override fun onCreate() {
        super.onCreate()
        // Enable Firebase offline capabilities
        database = FirebaseDatabase.getInstance().apply {
            setPersistenceEnabled(true)
        }
        auth = FirebaseAuth.getInstance()
    }
}