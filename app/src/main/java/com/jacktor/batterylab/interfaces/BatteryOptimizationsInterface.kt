package com.jacktor.batterylab.interfaces

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jacktor.batterylab.MainActivity
import com.jacktor.batterylab.R

interface BatteryOptimizationsInterface {

    fun MainActivity.isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(packageName) ?: false
    }

    fun MainActivity.showRequestIgnoringBatteryOptimizationsDialog() {
        showRequestIgnoringBatteryOptimizationsDialog =
            MaterialAlertDialogBuilder(this).apply {
                setIcon(R.drawable.ic_instruction_not_supported_24dp)
                setTitle(R.string.information)
                setMessage(R.string.ignoring_battery_optimizations_dialog_message)
                setPositiveButton(android.R.string.ok) { _, _ ->
                    isShowRequestIgnoringBatteryOptimizationsDialog = false
                    requestIgnoringBatteryOptimizations()
                }
                show()
            }
    }

    @SuppressLint("BatteryLife")
    private fun MainActivity.requestIgnoringBatteryOptimizations() {
        try {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
                startActivity(this)
            }
        }
        catch (_: ActivityNotFoundException) {}
    }
}