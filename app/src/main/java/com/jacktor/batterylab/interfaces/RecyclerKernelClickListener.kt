package com.jacktor.batterylab.interfaces

import com.jacktor.batterylab.views.KernelModel

interface RecyclerKernelClickListener {
    fun onItemClick(data: KernelModel, position: Int)
}