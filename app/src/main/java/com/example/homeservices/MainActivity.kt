package com.example.homeservices

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private val usersRef by lazy {
        FirebaseDatabase.getInstance().reference.child("Users")
    }

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        bindViews()
        setupListeners()
    }

    private fun bindViews() {
        emailInput = findViewById(R.id.etEmail)
        passwordInput = findViewById(R.id.etPassword)
        loginButton = findViewById(R.id.btnLogin)
    }

    private fun setupListeners() {
        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            signIn(email, password)
        }
    }

    private fun signIn(email: String, password: String) {
        if (!isValidInput(email, password)) return

        setLoading(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (!task.isSuccessful) {
                    setLoading(false)
                    showToast("Login failed: ${task.exception?.message}")
                    return@addOnCompleteListener
                }

                val userId = auth.currentUser?.uid

                if (userId == null) {
                    setLoading(false)
                    showToast("User not found")
                    return@addOnCompleteListener
                }

                loadUserRole(userId)
            }
    }

    private fun loadUserRole(userId: String) {
        usersRef.child(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                setLoading(false)

                val role = snapshot.child("role").value?.toString()

                when (role) {
                    "admin" -> openAdminScreen()
                    "customer", "user" -> openCustomerScreen()
                    else -> showToast("User role not found")
                }
            }
            .addOnFailureListener { error ->
                setLoading(false)
                showToast("Failed to load user role: ${error.message}")
            }
    }

    private fun isValidInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            showToast("Enter your email")
            return false
        }

        if (password.isEmpty()) {
            showToast("Enter your password")
            return false
        }

        return true
    }

    private fun openAdminScreen() {
        startActivity(Intent(this, AdminActivity::class.java))
        finish()
    }

    private fun openCustomerScreen() {
        startActivity(Intent(this, CustomerActivity::class.java))
        finish()
    }

    private fun setLoading(isLoading: Boolean) {
        loginButton.isEnabled = !isLoading
        loginButton.text = if (isLoading) "Please wait..." else "Login"
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}