package com.example.homeservices

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.homeservices.adapter.RequestAdapter
import com.example.homeservices.model.ServiceRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MyRequestsActivity : AppCompatActivity() {

    private lateinit var rvMyRequests: RecyclerView
    private lateinit var layoutNoRequests: View
    private lateinit var adapter: RequestAdapter

    private val requestsList = ArrayList<ServiceRequest>()
    private val db = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    private var listener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_requests)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.my_requests)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        rvMyRequests = findViewById(R.id.rvMyRequests)
        layoutNoRequests = findViewById(R.id.tvNoRequests)

        adapter = RequestAdapter(requestsList)
        rvMyRequests.layoutManager = LinearLayoutManager(this)
        rvMyRequests.adapter = adapter

        loadMyRequests()
    }

    private fun loadMyRequests() {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            Toast.makeText(this, "الرجاء تسجيل الدخول مرة أخرى", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                requestsList.clear()

                for (reqSnap in snapshot.children) {
                    val customerId = reqSnap.child("customerID").value?.toString() ?: ""

                    if (customerId != uid) continue

                    val request = ServiceRequest(
                        id = reqSnap.key ?: "",
                        customerID = customerId,
                        providerID = reqSnap.child("providerID").value?.toString() ?: "",
                        serviceID = reqSnap.child("serviceID").value?.toString() ?: "",
                        status = reqSnap.child("status").value?.toString() ?: "pending",
                        hoursRequested = reqSnap.child("hoursRequested").value?.toString()
                            ?.toDoubleOrNull()?.toInt() ?: 1,
                        timestamp = reqSnap.child("timestamp").value?.toString()
                            ?.toDoubleOrNull()?.toLong() ?: 0L
                    )

                    requestsList.add(request)
                }

                requestsList.sortByDescending { it.timestamp }
                adapter.notifyDataSetChanged()

                if (requestsList.isEmpty()) {
                    layoutNoRequests.visibility = View.VISIBLE
                    rvMyRequests.visibility = View.GONE
                } else {
                    layoutNoRequests.visibility = View.GONE
                    rvMyRequests.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@MyRequestsActivity,
                    "خطأ في الاتصال بقاعدة البيانات",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        db.child("ServiceRequests").addValueEventListener(listener as ValueEventListener)
    }

    override fun onDestroy() {
        super.onDestroy()

        listener?.let {
            db.child("ServiceRequests").removeEventListener(it)
        }
    }
}