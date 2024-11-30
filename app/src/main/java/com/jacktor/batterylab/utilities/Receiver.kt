package com.jacktor.batterylab.utilities

import android.content.Context
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.jacktor.batterylab.receivers.PowerConnectionReceiver

object Receiver {

    /**
     * Fungsi untuk mendaftarkan BroadcastReceiver dengan action tertentu.
     *
     * @param context Context aplikasi atau activity.
     * @param receiver Instance dari BroadcastReceiver.
     * @param unregister Instance dari BroadcastReceiver.
     * @param filter Intent Filter yang ingin didengarkan.
     */
    fun register(context: Context, receiver: PowerConnectionReceiver, filter: IntentFilter) {
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    fun unregister(context: Context, receiver: PowerConnectionReceiver) {
        try {
            context.unregisterReceiver(receiver)
        } catch (_: IllegalArgumentException) {
            // Receiver belum terdaftar
            //Log.d("Receiver", "Receiver is not registered")
        }
    }
}