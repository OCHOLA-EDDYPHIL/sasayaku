package com.example.chat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
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

        btnRegister.setOnClickListener {
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
        }
        btnLogin.setOnClickListener {
            val email = edtEmail.text.toString()
            val password = edtPassword.text.toString()

            if (validateInput(email, password)) {
                login(email, password)
            }
        }

        // Check if user is already logged in
        val sharedPreferences = getSharedPreferences("ChatApp", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
        if (isLoggedIn) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
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
                    // Save login state
                    val sharedPreferences = getSharedPreferences("ChatApp", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putBoolean("isLoggedIn", true)
                    editor.apply()

                    // Code for jumping to home activity
                    val intent = Intent(this@Login, MainActivity::class.java)
                    finish()
                    startActivity(intent)
                } else {
                    // If sign in fails, display a message to the user.
                    when (val exception = task.exception) {
                        is FirebaseAuthInvalidUserException -> {
                            AlertUtils.showAlert(
                                this,
                                "Login Failed",
                                "No account found with this email."
                            )
                        }

                        is FirebaseAuthInvalidCredentialsException -> {
                            AlertUtils.showAlert(this, "Login Failed", "Incorrect password.")
                        }

                        else -> {
                            AlertUtils.showAlert(
                                this,
                                "Login Failed",
                                "Some error occurred: ${exception?.message}"
                            )
                        }
                    }
                }
            }
    }
}