package com.jacktor.batterylab.interfaces

import com.jacktor.batterylab.views.KernelModel

interface RecyclerKernelCheckedChangeListener {
    fun onItemChecked(isChecked: Boolean, data: KernelModel, position: Int)
}