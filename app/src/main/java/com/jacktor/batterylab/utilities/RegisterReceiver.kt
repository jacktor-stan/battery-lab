package com.jacktor.batterylab.utilities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.core.content.ContextCompat

object RegisterReceiver {

    /**
     * Fungsi untuk mendaftarkan BroadcastReceiver dengan action tertentu.
     *
     * @param context Context aplikasi atau activity.
     * @param receiver Instance dari BroadcastReceiver.
     * @param action Action Intent yang ingin didengarkan.
     */
    fun register(context: Context, receiver: BroadcastReceiver, action: String) {
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(action),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }
}