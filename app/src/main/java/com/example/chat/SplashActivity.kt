package com.example.chat

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Delay for 3 seconds
        android.os.Handler().postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            TODO("splash not working yet")
            TODO("chat background feature not up yet")
            finish()
        }, 9000)
    }
}