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
import android.os.VibratorManager
import android.widget.Toast
import com.jacktor.batterylab.R
import com.jacktor.batterylab.utilities.Prefs


class PowerConnectionReceiver : BroadcastReceiver() {
    private var prefs: Prefs? = null

    override fun onReceive(context: Context, intent: Intent) {
        init(context)

        //prefsConfig!!.setInt("last_state_change_battery_level", getBatteryLevel(context))
        //prefsConfig!!.setLong("last_state_change_time", Date().time)

        //Log.d("SET LAST TIME:", Date().time.toString())

        //Sound delay pref
        val sdPref: String? = prefs!!.getString("sound_delay", "550")
        val sd = sdPref!!.toInt()
        val dur = prefs!!.getString("custom_vibrate_duration", "450")

        if (prefs!!.getBoolean("power_connection_service", false)) {
            if (intent.action == "android.intent.action.ACTION_POWER_CONNECTED") {
                val vibrationMode = prefs!!.getString("vibrate_mode", "disconnected")

                if (prefs!!.getBoolean("enable_vibration", true)) {
                    if (vibrationMode == "connected" || vibrationMode == "both") {
                        val v = dur!!.toLong() //Parse string ke int

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val vibratorManager =
                                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                                val vibrator = vibratorManager.defaultVibrator

                                vibrator.vibrate(
                                 VibrationEffect.createOneShot(v, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            val vibrator = context.getSystemService(VIBRATOR_SERVICE) as Vibrator
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator.vibrate(
                                    VibrationEffect.createOneShot(v, VibrationEffect.DEFAULT_AMPLITUDE))
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator.vibrate(v)
                            }
                        }


                    }
                }
                Handler(Looper.getMainLooper()).postDelayed(
                    {
                        rPowerConnected(context, prefs!!) //Putar suara
                    },
                    sd.toLong()
                )
                if (prefs!!.getBoolean("enable_toast", false)) {
                    Toast.makeText(
                        context.applicationContext,
                        R.string.toast_power_connected,
                        Toast.LENGTH_LONG
                    ).show() //Tampilkan toast
                }
            } else if (intent.action == "android.intent.action.ACTION_POWER_DISCONNECTED") {
                val vibrationMode = prefs!!.getString("vibrate_mode", "disconnected")
                if (prefs!!.getBoolean("enable_vibration", true)) {
                    if (vibrationMode == "disconnected" || vibrationMode == "both") {
                        val v = dur!!.toLong() //Parse string ke int

                        val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val vibratorManager =
                                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                            vibratorManager.defaultVibrator
                        } else {
                            // backward compatibility for Android API < 31,
                            @Suppress("DEPRECATION")
                            context.getSystemService(VIBRATOR_SERVICE) as Vibrator
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vib.vibrate(
                                VibrationEffect.createOneShot(v, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            // backward compatibility for Android API < 26
                            @Suppress("DEPRECATION")
                            vib.vibrate(v)
                        }


                    }
                }

                Handler(Looper.getMainLooper()).postDelayed(
                    {
                        rPowerDisconnected(context, prefs!!) //Putar suara
                    },
                    sd.toLong()
                )
                //550
                if (prefs!!.getBoolean("enable_toast", false)) {
                    Toast.makeText(
                        context.applicationContext,
                        R.string.toast_power_disconnected,
                        Toast.LENGTH_LONG
                    ).show() //Tampilkan toast
                }
            }
        }
    }

    //Terhubung
    private fun rPowerConnected(context: Context, prefs: Prefs) {
        val ac: Boolean
        val bat = context.applicationContext.registerReceiver(
            null,
            IntentFilter("android.intent.action.BATTERY_CHANGED")
        )
            ac = bat!!.getIntExtra("plugged", 1) == 1
        addSound(
            context,
            prefs,
            if (ac) "ac_connected_sound" else "usb_connected_sound"
        )
    }

    private fun rPowerDisconnected(context: Context, prefs: Prefs) {
        addSound(context, prefs, "disconnected_sound")
    }

    private fun addSound(context: Context, prefs: Prefs, prefKey: String) {
        val url = prefs.getString(prefKey, "")
        if (url != "") {
            val filePath = prefs.getString(prefKey, "")
            try {
                val r = RingtoneManager.getRingtone(context, Uri.parse(filePath))
                r.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun init(context: Context) {
        prefs = Prefs(context)
    }
}