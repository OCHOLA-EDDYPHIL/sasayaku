package com.example.chat

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class Login : AppCompatActivity() {

    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var chkRememberMe: CheckBox

    companion object {
        private const val PREFS_NAME = "ChatApp"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_REMEMBER_ME = "rememberMe"
        private const val KEY_LAST_LOGIN_TIME = "lastLoginTime"
        private const val TIMEOUT_DURATION = 24 * 60 * 60 * 1000 // 24 hours
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        supportActionBar?.hide()

        auth = TubongeDb.getAuth()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        edtEmail = findViewById(R.id.edt_email)
        edtPassword = findViewById(R.id.edt_password)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)
        chkRememberMe = findViewById(R.id.chk_remember_me)

        btnRegister.setOnClickListener {
            startActivity(Intent(this, Register::class.java))
        }

        btnLogin.setOnClickListener {
            val email = edtEmail.text.toString()
            val password = edtPassword.text.toString()

            if (validateInput(email, password)) {
                login(email, password)
            }
        }

        checkLoginStatus()
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            edtEmail.error = "Email is required"
            return false
        }
        if (password.isEmpty()) {
            edtPassword.error = "Password is required"
            return false
        }
        return true
    }

    private fun login(email: String, password: String) {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            AlertUtils.showAlert(this, "Login Failed", "No internet connection.")
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    saveLoginState()
                    startActivity(Intent(this@Login, MainActivity::class.java))
                    finish()
                } else {
                    handleLoginFailure(task.exception)
                }
            }
    }

    private fun saveLoginState() {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putBoolean(KEY_REMEMBER_ME, chkRememberMe.isChecked)
            putLong(KEY_LAST_LOGIN_TIME, System.currentTimeMillis())
            apply()
        }
    }

    private fun handleLoginFailure(exception: Exception?) {
        when (exception) {
            is FirebaseAuthInvalidUserException -> {
                AlertUtils.showAlert(
                    this, "Login Failed",
                    "No account found with this email."
                )
            }

                        is FirebaseAuthInvalidCredentialsException -> {
                            AlertUtils.showAlert(this, "Login Failed", "Incorrect password.")
                        }

            else -> {
                AlertUtils.showAlert(
                    this, "Login Failed",
                    "Some error occurred: ${exception?.message}"
                )
            }
        }
    }

    private fun checkLoginStatus() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
        val rememberMe = sharedPreferences.getBoolean(KEY_REMEMBER_ME, false)

        if (isLoggedIn) {
            if (!rememberMe) {
                handleTimeout(sharedPreferences)
            } else {
                navigateToMainActivity()
            }
        }
    }

    private fun handleTimeout(sharedPreferences: SharedPreferences) {
        val lastLoginTime = sharedPreferences.getLong(KEY_LAST_LOGIN_TIME, 0)
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastLoginTime > TIMEOUT_DURATION) {
            sharedPreferences.edit().apply {
                putBoolean(KEY_IS_LOGGED_IN, false)
                apply()
            }
        } else {
            navigateToMainActivity()
        }
    }

    private fun navigateToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}