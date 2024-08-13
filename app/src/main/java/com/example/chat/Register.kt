package com.example.chat

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
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

class Register : AppCompatActivity() {

    private lateinit var edtName: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var btnLogin: TextView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        supportActionBar?.hide()

        auth = TubongeDb.getAuth()

        edtName = findViewById(R.id.edt_name)
        edtEmail = findViewById(R.id.edt_email)
        edtPassword = findViewById(R.id.edt_password)
        btnRegister = findViewById(R.id.btnRegister)
        btnLogin = findViewById(R.id.btnLogin)

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
        if (name.isEmpty()) {
            edtName.error = "Name is required"
            return false
        }
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

    private fun register(name: String, email: String, password: String) {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            AlertUtils.showAlert(this, "Registration Failed", "No internet connection.")
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    addUserToDatabase(name, email, auth.currentUser?.uid!!)
                    val intent = Intent(this@Register, MainActivity::class.java)
                    finish()
                    startActivity(intent)
                } else {
                    val exception = task.exception
                    when (exception) {
                        is FirebaseAuthUserCollisionException -> {
                            AlertUtils.showAlert(
                                this,
                                "Registration Failed",
                                "This email is already registered."
                            )
                        }

                        is FirebaseAuthWeakPasswordException -> {
                            AlertUtils.showAlert(
                                this,
                                "Registration Failed",
                                "Password is too weak."
                            )
                        }

                        is FirebaseAuthInvalidCredentialsException -> {
                            AlertUtils.showAlert(
                                this,
                                "Registration Failed",
                                "Invalid email format."
                            )
                        }

                        else -> {
                            AlertUtils.showAlert(
                                this,
                                "Registration Failed",
                                "Some error occurred: ${exception?.message}"
                            )
                        }
                    }
                }
            }
    }

    private fun addUserToDatabase(name: String, email: String, uid: String) {
        val mDbRef = TubongeDb.getDatabase().getReference()
        mDbRef.child("user").child(uid).setValue(User(name, email, uid))
    }
}