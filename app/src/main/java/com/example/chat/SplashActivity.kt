package com.example.chat

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    companion object {
        const val SPLASH_DELAY = 3000L // 3 seconds delay
    }

    private val splashViewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Make the splash screen full screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        }

        splashViewModel.startSplashScreen(this)
    }
}

class SplashViewModel : ViewModel() {

    fun startSplashScreen(activity: SplashActivity) {
        activity.lifecycleScope.launch {
            delay(SplashActivity.SPLASH_DELAY)
            val user = FirebaseAuth.getInstance().currentUser
            val intent = if (user != null) {
                Intent(activity, MainActivity::class.java)
            } else {
                Intent(activity, Login::class.java)
            }
            activity.startActivity(intent)
            activity.finish()
        }
    }
}