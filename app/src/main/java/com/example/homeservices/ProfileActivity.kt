package com.example.homeservices

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ProfileActivity : AppCompatActivity() {

    private lateinit var ivProfileImage: ImageView
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileEmail: TextView
    private lateinit var tvProfilePhone: TextView
    private lateinit var tvProfileAddress: TextView

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Profile"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        ivProfileImage = findViewById(R.id.ivProfileImage)
        tvProfileName = findViewById(R.id.tvProfileName)
        tvProfileEmail = findViewById(R.id.tvProfileEmail)
        tvProfilePhone = findViewById(R.id.tvProfilePhone)
        tvProfileAddress = findViewById(R.id.tvProfileAddress)

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            auth.signOut()
            finish()
        }

        loadUserProfile()
    }

    private fun loadUserProfile() {
        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        db.child("Users")
            .child(user.uid)
            .get()
            .addOnSuccessListener { snapshot ->

                if (!snapshot.exists()) {
                    Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                tvProfileName.text = snapshot.child("name").value?.toString() ?: ""
                tvProfileEmail.text = snapshot.child("email").value?.toString() ?: user.email ?: ""
                tvProfilePhone.text = snapshot.child("phone").value?.toString() ?: ""
                tvProfileAddress.text = snapshot.child("address").value?.toString() ?: ""

                val imageUrl = snapshot.child("profileImage").value?.toString() ?: ""

                if (imageUrl.isNotEmpty() && imageUrl != "null") {
                    Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_launcher_background)
                        .into(ivProfileImage)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
    }
}