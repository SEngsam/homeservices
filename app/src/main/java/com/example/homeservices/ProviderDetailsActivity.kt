package com.example.homeservices

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.example.homeservices.model.ServiceProvider
import com.example.homeservices.model.ServiceRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ProviderDetailsActivity : AppCompatActivity() {

    private lateinit var ivProviderImage: ImageView
    private lateinit var tvProviderName: TextView
    private lateinit var tvProviderDescription: TextView
    private lateinit var tvProviderRate: TextView
    private lateinit var tvProviderPrice: TextView
    private lateinit var tvProviderAddress: TextView
    private lateinit var btnRequestService: Button
    private lateinit var btnViewLocation: Button

    private var provider: ServiceProvider? = null

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_provider_details)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        ivProviderImage = findViewById(R.id.ivProviderImage)
        tvProviderName = findViewById(R.id.tvProviderName)
        tvProviderDescription = findViewById(R.id.tvProviderDescription)
        tvProviderRate = findViewById(R.id.tvProviderRate)
        tvProviderPrice = findViewById(R.id.tvProviderPrice)
        tvProviderAddress = findViewById(R.id.tvProviderAddress)

        btnRequestService = findViewById(R.id.btnRequestService)
        btnViewLocation = findViewById(R.id.btnViewLocation)

        provider =
            intent.getSerializableExtra("provider") as? ServiceProvider

        if (provider == null) {
            Toast.makeText(this, "Provider not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadProviderData()

        btnRequestService.setOnClickListener {
            requestService()
        }

        btnViewLocation.setOnClickListener {
            openLocation()
        }
    }

    private fun loadProviderData() {

        val item = provider ?: return

        tvProviderName.text = item.name
        tvProviderDescription.text = item.description
        tvProviderRate.text = "⭐ ${item.rate}"
        tvProviderPrice.text = "${item.pricePerHour}$ / hour"
        tvProviderAddress.text = item.address

        if (item.image.isNotEmpty()) {

            Glide.with(this)
                .load(item.image)
                .placeholder(R.drawable.ic_launcher_background)
                .into(ivProviderImage)

        } else {

            ivProviderImage.setImageResource(
                R.drawable.ic_launcher_background
            )
        }
    }

    private fun requestService() {

        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(this, "يرجى تسجيل الدخول", Toast.LENGTH_SHORT).show()
            return
        }

        val item = provider ?: return

        val requestId =
            db.child("ServiceRequests").push().key ?: return

        val request = ServiceRequest(
            id = requestId,
            customerID = currentUser.uid,
            providerID = item.id,
            serviceID = item.categoryID,
            status = "pending",
            hoursRequested = 1,
            timestamp = System.currentTimeMillis()
        )

        db.child("ServiceRequests")
            .child(requestId)
            .setValue(request)
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "تم إرسال الطلب",
                    Toast.LENGTH_SHORT
                ).show()

                startActivity(
                    Intent(this, MyRequestsActivity::class.java)
                )

                finish()
            }
            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "حدث خطأ",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun openLocation() {

        val location = provider?.location

        if (location == null) {
            Toast.makeText(this, "الموقع غير متوفر", Toast.LENGTH_SHORT).show()
            return
        }

        val uri =
            Uri.parse(
                "google.navigation:q=${location.latitude},${location.longitude}"
            )

        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")

        try {

            startActivity(intent)

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Google Maps غير موجود",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}