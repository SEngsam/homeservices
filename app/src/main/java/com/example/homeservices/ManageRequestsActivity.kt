package com.example.homeservices

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.homeservices.adapter.AdminRequestAdapter
import com.example.homeservices.model.ServiceRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ManageRequestsActivity : AppCompatActivity() {

    private lateinit var requestsRecycler: RecyclerView
    private lateinit var requestAdapter: AdminRequestAdapter

    private val requests = ArrayList<ServiceRequest>()

    private val requestsRef by lazy {
        FirebaseDatabase.getInstance().reference.child("ServiceRequests")
    }

    private var requestsListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_requests)

        setupToolbar()
        setupRecyclerView()
        observeRequests()
    }

    override fun onDestroy() {
        super.onDestroy()

        requestsListener?.let {
            requestsRef.removeEventListener(it)
        }
    }

    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)

        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            title = "إدارة الطلبات"
            setDisplayHomeAsUpEnabled(true)
        }

        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        requestsRecycler = findViewById(R.id.rvManageRequests)

        requestAdapter = AdminRequestAdapter(requests) { request ->
            completeRequest(request)
        }

        requestsRecycler.apply {
            layoutManager = LinearLayoutManager(this@ManageRequestsActivity)
            adapter = requestAdapter
        }
    }

    private fun observeRequests() {
        requestsListener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                requests.clear()

                snapshot.children.forEach { item ->
                    mapRequest(item)?.let { request ->
                        requests.add(request)
                    }
                }

                requests.sortByDescending { it.timestamp }
                requestAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("فشل تحميل البيانات: ${error.message}")
            }
        }

        requestsRef.addValueEventListener(requestsListener as ValueEventListener)
    }

    private fun mapRequest(snapshot: DataSnapshot): ServiceRequest? {
        return try {
            ServiceRequest(
                id = snapshot.key.orEmpty(),
                customerID = snapshot.child("customerID").asString(),
                providerID = snapshot.child("providerID").asString(),
                serviceID = snapshot.child("serviceID").asString(),
                status = snapshot.child("status").asString("pending"),
                hoursRequested = snapshot.child("hoursRequested").asInt(1),
                timestamp = snapshot.child("timestamp").asLong()
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun completeRequest(request: ServiceRequest) {
        if (request.status == "completed") {
            showToast("تم إكمال هذا الطلب مسبقاً")
            return
        }

        requestsRef.child(request.id)
            .child("status")
            .setValue("completed")
            .addOnSuccessListener {
                showToast("تمت الموافقة على الطلب بنجاح")
            }
            .addOnFailureListener {
                showToast("حدث خطأ أثناء تحديث حالة الطلب")
            }
    }

    private fun DataSnapshot.asString(defaultValue: String = ""): String {
        return value?.toString() ?: defaultValue
    }

    private fun DataSnapshot.asInt(defaultValue: Int = 0): Int {
        return value?.toString()?.toDoubleOrNull()?.toInt() ?: defaultValue
    }

    private fun DataSnapshot.asLong(defaultValue: Long = 0L): Long {
        return value?.toString()?.toDoubleOrNull()?.toLong() ?: defaultValue
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}