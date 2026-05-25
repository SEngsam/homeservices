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

class AdminRequestAdapter(
    private val requests: List<ServiceRequest>,
    private val onApproveClick: (ServiceRequest) -> Unit
) : RecyclerView.Adapter<AdminRequestAdapter.AdminRequestViewHolder>() {

    class AdminRequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCustomerName: TextView = itemView.findViewById(R.id.tvProviderId)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvHours: TextView = itemView.findViewById(R.id.tvHours)
        val btnApprove: Button = itemView.findViewById(R.id.btnDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminRequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_request, parent, false)
        return AdminRequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdminRequestViewHolder, position: Int) {
        val request = requests[position]

        // Translate status
        val statusDisplay = when(request.status.lowercase()) {
            "pending" -> "قيد الانتظار"
            "completed" -> "مكتمل"
            else -> request.status
        }

        holder.tvStatus.text = statusDisplay
        holder.tvHours.text = "${request.hoursRequested} ساعة"
        
        if (request.status == "completed") {
            holder.btnApprove.text = "تمت الموافقة"
            holder.btnApprove.isEnabled = false
            holder.tvStatus.setTextColor(holder.itemView.context.getColor(android.R.color.holo_green_dark))
        } else {
            holder.btnApprove.text = "موافقة"
            holder.btnApprove.isEnabled = true
            holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.brand_orange))
        }

        // Fetch customer name
        FirebaseDatabase.getInstance().reference
            .child("Users")
            .child(request.customerID)
            .get()
            .addOnSuccessListener { snapshot ->
                val customerName = snapshot.child("name").value?.toString() ?: "عميل غير معروف"
                holder.tvCustomerName.text = "العميل: $customerName"
            }
            .addOnFailureListener {
                holder.tvCustomerName.text = "خطأ في تحميل الاسم"
            }

        holder.btnApprove.setOnClickListener {
            onApproveClick(request)
        }
    }

    override fun getItemCount(): Int = requests.size
}