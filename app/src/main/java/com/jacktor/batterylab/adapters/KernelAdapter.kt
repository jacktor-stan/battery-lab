package com.jacktor.batterylab.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
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
    private val dataList: MutableList<KernelModel>,
    private val context: Context,
    private val recyclerKernelClickListener: RecyclerKernelClickListener,
    private val recyclerKernelCheckedChangeListener: RecyclerKernelCheckedChangeListener
) : RecyclerView.Adapter<KernelAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewKernelName: TextView = itemView.findViewById(R.id.value)
        val textViewAvailable: TextView = itemView.findViewById(R.id.available)
        val textViewStored: TextView = itemView.findViewById(R.id.stored_value)
        val textViewStatus: TextView = itemView.findViewById(R.id.kernel_status)
        val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
        val enableScript: MaterialSwitch = itemView.findViewById(R.id.enable_script)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_view_design_kernel, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentKernel = dataList[position]
        val pref = Prefs(context)

        // Helper untuk mengatur tampilan teks
        fun setUpTextAppearance(view: TextView) {
            TextAppearanceHelper.setTextAppearance(
                context,
                view as AppCompatTextView,
                pref.getString(PreferencesKeys.TEXT_STYLE, "0"),
                pref.getString(PreferencesKeys.TEXT_FONT, "6"),
                pref.getString(PreferencesKeys.TEXT_SIZE, "2"),
                false
            )
        }

        with(holder) {
            // Atur tampilan teks
            setUpTextAppearance(textViewKernelName)
            setUpTextAppearance(textViewStored)
            setUpTextAppearance(textViewStatus)
            setUpTextAppearance(textViewAvailable)

            // Atur nilai dari data
            textViewKernelName.text = currentKernel.list
            textViewStored.text = currentKernel.stored
            textViewStatus.text = currentKernel.kernelStatus
            textViewAvailable.text = currentKernel.available
            cardView.cardElevation = currentKernel.cardElevation
            enableScript.isChecked = currentKernel.isChecked

            // Event listener
            itemView.setOnClickListener {
                recyclerKernelClickListener.onItemClick(currentKernel, position)
            }
            enableScript.setOnClickListener {
                recyclerKernelCheckedChangeListener.onItemChecked(
                    enableScript.isChecked,
                    currentKernel,
                    position
                )
            }
        }
    }

    override fun getItemCount(): Int = dataList.size
}
