package com.jacktor.batterylab.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.jacktor.batterylab.R
import com.jacktor.batterylab.helpers.TextAppearanceHelper
import com.jacktor.batterylab.interfaces.RecyclerKernelCheckedChangeListener
import com.jacktor.batterylab.interfaces.RecyclerKernelClickListener
import com.jacktor.batterylab.utilities.PreferencesKeys
import com.jacktor.batterylab.utilities.Prefs
import com.jacktor.batterylab.views.KernelModel


class KernelAdapter(
    data: ArrayList<KernelModel>,
    context: Context,
    private var recyclerKernelClickListener: RecyclerKernelClickListener,
    private var recyclerKernelCheckedChangeListener: RecyclerKernelCheckedChangeListener
) :
    RecyclerView.Adapter<KernelAdapter.ViewHolder>() {
    private var dataList: ArrayList<KernelModel> = ArrayList()
    private val context: Context

    init {
        dataList = data
        this.context = context
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var textViewKernelName: TextView
        var textViewAvailable: TextView
        var textViewStored: TextView
        var textViewStatus: TextView
        var cardView: MaterialCardView
        var enableScript: MaterialSwitch

        init {
            textViewKernelName = itemView.findViewById(R.id.value)
            textViewStored = itemView.findViewById(R.id.stored_value)
            textViewStatus = itemView.findViewById(R.id.kernel_status)
            textViewAvailable = itemView.findViewById(R.id.available)
            cardView = itemView.findViewById(R.id.cardView)
            enableScript = itemView.findViewById(R.id.enable_script)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_view_design_kernel, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pref = Prefs(context)

        TextAppearanceHelper.setTextAppearance(
            context, holder.cardView.findViewById(R.id.value),
            pref.getString(PreferencesKeys.TEXT_STYLE, "0"),
            pref.getString(PreferencesKeys.TEXT_FONT, "6"),
            null
        )
        TextAppearanceHelper.setTextAppearance(
            context, holder.cardView.findViewById(R.id.available),
            pref.getString(PreferencesKeys.TEXT_STYLE, "0"),
            pref.getString(PreferencesKeys.TEXT_FONT, "6"),
            null
        )

        holder.textViewKernelName.text = dataList[position].list
        holder.textViewStored.text = dataList[position].stored
        holder.textViewStatus.text = dataList[position].kernelStatus
        holder.textViewAvailable.text = dataList[position].available
        holder.cardView.cardElevation = dataList[position].cardElevation
        holder.enableScript.isChecked = dataList[position].isChecked

        holder.itemView.setOnClickListener {
            recyclerKernelClickListener.onItemClick(dataList[position], position)
        }

        holder.enableScript.setOnClickListener {
            recyclerKernelCheckedChangeListener.onItemChecked(
                holder.enableScript.isChecked,
                dataList[position], position
            )
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }
}