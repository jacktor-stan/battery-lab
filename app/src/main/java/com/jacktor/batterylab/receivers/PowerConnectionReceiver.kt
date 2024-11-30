package com.jacktor.batterylab.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import com.jacktor.batterylab.R
import com.jacktor.batterylab.utilities.preferences.PreferencesKeys.AC_CONNECTED_SOUND
import com.jacktor.batterylab.utilities.preferences.PreferencesKeys.CUSTOM_VIBRATE_DURATION
import com.jacktor.batterylab.utilities.preferences.PreferencesKeys.DISCONNECTED_SOUND
import com.jacktor.batterylab.utilities.preferences.PreferencesKeys.ENABLE_TOAST
import com.jacktor.batterylab.utilities.preferences.PreferencesKeys.ENABLE_VIBRATION
import com.jacktor.batterylab.utilities.preferences.PreferencesKeys.SOUND_DELAY
import com.jacktor.batterylab.utilities.preferences.PreferencesKeys.USB_CONNECTED_SOUND
import com.jacktor.batterylab.utilities.preferences.PreferencesKeys.VIBRATE_MODE
import com.jacktor.batterylab.utilities.preferences.Prefs

class PowerConnectionReceiver : BroadcastReceiver() {
    private lateinit var prefs: Prefs

    override fun onReceive(context: Context, intent: Intent) {
        init(context)

        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> handlePowerChange(
                context, isConnected = true
            )

            Intent.ACTION_POWER_DISCONNECTED -> handlePowerChange(
                context, isConnected = false
            )
        }
    }

    private fun handlePowerChange(context: Context, isConnected: Boolean) {
        val vibrationMode = prefs.getString(VIBRATE_MODE, "disconnected")
        val duration = prefs.getString(CUSTOM_VIBRATE_DURATION, "450")!!.toLong()
        val delay = prefs.getString(SOUND_DELAY, "550")!!.toLong()

        if (prefs.getBoolean(ENABLE_VIBRATION, true)) {
            handleVibration(context, duration, isConnected, vibrationMode)
        }

        Handler(Looper.getMainLooper()).postDelayed({
            if (isConnected) {
                playPowerConnectedSound(context)
            } else {
                playPowerDisconnectedSound(context)
            }
        }, delay)

        if (prefs.getBoolean(ENABLE_TOAST, false)) {
            showToast(context, isConnected)
        }
    }

    private fun handleVibration(
        context: Context, duration: Long, isConnected: Boolean, vibrationMode: String?
    ) {
        if ((isConnected && vibrationMode in listOf("connected", "both")) ||
            (!isConnected && vibrationMode in listOf("disconnected", "both"))
        ) {
            val vibrator = getVibrator(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        }
    }

    private fun getVibrator(context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun playPowerConnectedSound(context: Context) {
        val isAcConnected = isAcConnected(context)
        val prefKey = if (isAcConnected) AC_CONNECTED_SOUND else USB_CONNECTED_SOUND
        playSound(context, prefKey)
    }

    private fun playPowerDisconnectedSound(context: Context) {
        playSound(context, DISCONNECTED_SOUND)
    }

    private fun playSound(context: Context, prefKey: String) {
        val filePath = prefs.getString(prefKey, "") ?: return
        if (filePath.isNotEmpty()) {
            try {
                val ringtone = RingtoneManager.getRingtone(context, Uri.parse(filePath))
                ringtone.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun isAcConnected(context: Context): Boolean {
        val batteryStatus = context.registerReceiver(
            null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        return batteryStatus?.getIntExtra("plugged", -1) == 1
    }

    private fun showToast(context: Context, isConnected: Boolean) {
        val messageResId = if (isConnected) {
            R.string.toast_power_connected
        } else {
            R.string.toast_power_disconnected
        }
        Toast.makeText(context.applicationContext, messageResId, Toast.LENGTH_LONG).show()
    }

    private fun init(context: Context) {
        prefs = Prefs(context)
    }
}
