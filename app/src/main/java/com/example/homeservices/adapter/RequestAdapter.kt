package com.example.homeservices.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.homeservices.R
import com.example.homeservices.model.ServiceRequest
import com.google.firebase.database.FirebaseDatabase

class RequestAdapter(
    private val requests: List<ServiceRequest>
) : RecyclerView.Adapter<RequestAdapter.RequestViewHolder>() {

    class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvProviderId: TextView = itemView.findViewById(R.id.tvProviderId)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvHours: TextView = itemView.findViewById(R.id.tvHours)
        val btnDetails: Button = itemView.findViewById(R.id.btnDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val request = requests[position]

        // Hide details button for customer as it might not be needed or lead to nowhere
        holder.btnDetails.visibility = View.GONE

        // Translate status
        val statusDisplay = when(request.status.lowercase()) {
            "pending" -> "قيد الانتظار"
            "completed" -> "مكتمل"
            else -> request.status
        }
        
        holder.tvStatus.text = statusDisplay
        holder.tvHours.text = "${request.hoursRequested} ساعة"

        // Change status color
        if (request.status == "completed") {
            holder.tvStatus.setTextColor(holder.itemView.context.getColor(android.R.color.holo_green_dark))
        } else {
            holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.brand_orange))
        }

        // Load Provider Name
        FirebaseDatabase.getInstance().reference
            .child("ServiceProviders")
            .child(request.providerID)
            .get()
            .addOnSuccessListener { snapshot ->
                val providerName = snapshot.child("name").value?.toString() ?: "مزود خدمة"
                holder.tvProviderId.text = providerName
            }
            .addOnFailureListener {
                holder.tvProviderId.text = "مزود غير معروف"
            }
    }

    override fun getItemCount(): Int = requests.size
}