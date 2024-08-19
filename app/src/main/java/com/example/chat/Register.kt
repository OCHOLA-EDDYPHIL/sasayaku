package com.example.chat

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

class Register : AppCompatActivity() {

    private lateinit var edtName: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var btnLogin: TextView
    private lateinit var auth: FirebaseAuth

    companion object {
        const val ERROR_NAME_REQUIRED = "Name is required"
        const val ERROR_EMAIL_REQUIRED = "Email is required"
        const val ERROR_PASSWORD_REQUIRED = "Password is required"
        const val ERROR_NO_INTERNET = "No internet connection."
        const val ERROR_EMAIL_REGISTERED = "This email is already registered."
        const val ERROR_WEAK_PASSWORD = "Password is too weak."
        const val ERROR_INVALID_EMAIL = "Invalid email format."
        const val ERROR_GENERIC = "Some error occurred: "
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        supportActionBar?.hide()

        auth = TubongeDb.getAuth()

        initViews()
        setListeners()
    }

    private fun initViews() {
        edtName = findViewById(R.id.edt_name)
        edtEmail = findViewById(R.id.edt_email)
        edtPassword = findViewById(R.id.edt_password)
        btnRegister = findViewById(R.id.btnRegister)
        btnLogin = findViewById(R.id.btnLogin)
    }

    private fun setListeners() {
        btnRegister.setOnClickListener {
            val name = edtName.text.toString()
            val email = edtEmail.text.toString()
            val password = edtPassword.text.toString()

            if (validateInput(name, email, password)) {
                register(name, email, password)
            }
        }
        btnLogin.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun validateInput(name: String, email: String, password: String): Boolean {
        return when {
            name.isEmpty() -> {
                edtName.error = ERROR_NAME_REQUIRED
                false
            }

            email.isEmpty() -> {
                edtEmail.error = ERROR_EMAIL_REQUIRED
                false
            }

            password.isEmpty() -> {
                edtPassword.error = ERROR_PASSWORD_REQUIRED
                false
            }

            else -> true
        }
    }

    private fun register(name: String, email: String, password: String) {
        if (!isNetworkAvailable()) {
            showAlert(ERROR_NO_INTERNET)
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    addUserToDatabase(name, email, auth.currentUser?.uid!!)
                    navigateToMainActivity()
                } else {
                    handleRegistrationError(task.exception)
                }
            }
    }

    private fun isNetworkAvailable(): Boolean {
        return NetworkUtils.isNetworkAvailable(this)
    }

    private fun showAlert(message: String) {
        AlertUtils.showAlert(this, "Registration Failed", message)
    }

    private fun handleRegistrationError(exception: Exception?) {
        when (exception) {
            is FirebaseAuthUserCollisionException -> showAlert(ERROR_EMAIL_REGISTERED)
            is FirebaseAuthWeakPasswordException -> showAlert(ERROR_WEAK_PASSWORD)
            is FirebaseAuthInvalidCredentialsException -> showAlert(ERROR_INVALID_EMAIL)
            else -> showAlert(ERROR_GENERIC + (exception?.message ?: ""))
        }
    }

    private fun addUserToDatabase(name: String, email: String, uid: String) {
        val mDbRef = TubongeDb.getDatabase().getReference()
        mDbRef.child("user").child(uid).setValue(User(name, email, uid, null))
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this@Register, MainActivity::class.java)
        finish()
        startActivity(intent)
        // make a toast to welcome the user
        Toast.makeText(this, "Registration successful.", Toast.LENGTH_SHORT).show()
    }
}