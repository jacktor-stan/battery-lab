package com.jacktor.batterylab.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.ProductDetails
import com.jacktor.batterylab.R
import com.jacktor.batterylab.interfaces.PremiumInterface.Companion.isPremium
import com.jacktor.batterylab.interfaces.RecyclerPremiumInterface


class PremiumAdapter(
    var context: Context,
    private var productDetailsList: List<ProductDetails>,
    var recyclerPremiumInterface: RecyclerPremiumInterface
) :
    RecyclerView.Adapter<PremiumAdapter.BuyPremiumViewHolder?>() {
    //var TAG = "TestINAPP"
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuyPremiumViewHolder {
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.premium_item, parent, false)
        return BuyPremiumViewHolder(view)
    }

    override fun onBindViewHolder(holder: BuyPremiumViewHolder, position: Int) {
        val currentItem = productDetailsList[position]

        if (isPremium) {
            holder.txtPremiumTitle.text = context.getString(R.string.already_purchased)
        } else {
            holder.txtPremiumTitle.text = currentItem.name
        }

        holder.txtPremiumPrice.text = currentItem.oneTimePurchaseOfferDetails?.formattedPrice

    }

    override fun getItemCount(): Int {
        val limit = 1
        return if (productDetailsList.size > limit) {
            limit
        } else {
            productDetailsList.size
        }
    }

    inner class BuyPremiumViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var txtPremiumTitle: TextView
        var txtPremiumPrice: TextView

        init {
            txtPremiumTitle = itemView.findViewById(R.id.product_name)
            txtPremiumPrice = itemView.findViewById(R.id.product_price)
            itemView.setOnClickListener {
                recyclerPremiumInterface.onItemClick(bindingAdapterPosition)
            }
        }
    }
}
