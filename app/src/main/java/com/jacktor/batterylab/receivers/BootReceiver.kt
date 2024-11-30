package com.jacktor.batterylab.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.jacktor.batterylab.R
import com.jacktor.batterylab.helpers.ServiceHelper
import com.jacktor.batterylab.interfaces.OverlayInterface
import com.jacktor.batterylab.services.BatteryLabService
import com.jacktor.batterylab.services.OverlayService
import com.jacktor.batterylab.utilities.Constants
import com.jacktor.batterylab.utilities.preferences.PreferencesKeys.AUTO_START_BOOT
import com.jacktor.batterylab.utilities.preferences.PreferencesKeys.EXECUTE_SCRIPT_ON_BOOT
import com.topjohnwu.superuser.Shell

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        when (intent.action) {

            Intent.ACTION_BOOT_COMPLETED, "android.intent.action.QUICKBOOT_POWERON" -> {

                val pref = PreferenceManager.getDefaultSharedPreferences(context)

                if (!pref.getBoolean(
                        AUTO_START_BOOT, context.resources.getBoolean(
                            R.bool
                                .auto_start_boot
                        )
                    )
                ) return

                ServiceHelper.cancelAllJobs(context)

                if (BatteryLabService.instance == null &&
                    !ServiceHelper.isStartedBatteryLabService()
                ) ServiceHelper.startService(
                    context, BatteryLabService::class.java
                )

                if (OverlayService.instance == null && OverlayInterface.isEnabledOverlay(context)
                    && !ServiceHelper.isStartedOverlayService()
                )
                    ServiceHelper.startService(context, OverlayService::class.java)

                //Apply script
                if (pref.getBoolean(
                        EXECUTE_SCRIPT_ON_BOOT, context.resources.getBoolean(
                            R.bool
                                .execute_script_on_boot
                        )
                    )
                ) {
                    Shell.cmd("su && sh ${context.filesDir.path}/${Constants.SCRIPT_FILE_NAME}")
                        .exec().apply {
                            if (isSuccess) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.app_name) + ": " + context.getString(
                                        R.string.executing_script
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.app_name) + ": " + context.getString(
                                        R.string.execute_failed
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                }

            }
        }
    }
}