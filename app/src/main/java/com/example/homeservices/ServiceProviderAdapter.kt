package com.example.homeservices.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.homeservices.R
import com.example.homeservices.model.ServiceProvider

class ServiceProviderAdapter(
    private val providers: List<ServiceProvider>,
    private val onClick: (ServiceProvider) -> Unit
) : RecyclerView.Adapter<ServiceProviderAdapter.ProviderViewHolder>() {

    inner class ProviderViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val image: ImageView =
            itemView.findViewById(R.id.ivProvider)

        val name: TextView =
            itemView.findViewById(R.id.tvProviderName)

        val rate: TextView =
            itemView.findViewById(R.id.tvRate)

        val price: TextView =
            itemView.findViewById(R.id.tvPrice)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ProviderViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_provider, parent, false)

        return ProviderViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ProviderViewHolder,
        position: Int
    ) {

        val provider = providers[position]

        holder.name.text = provider.name
        holder.price.text = "${provider.pricePerHour} / hour"
        holder.rate.text = "⭐ ${provider.rate}"

        if (provider.image.isNotEmpty()) {

            Glide.with(holder.itemView.context)
                .load(provider.image)
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.image)

        } else {

            holder.image.setImageResource(
                R.drawable.ic_launcher_background
            )
        }

        holder.itemView.setOnClickListener {
            onClick(provider)
        }
    }

    override fun getItemCount(): Int {
        return providers.size
    }
}