package com.jacktor.batterylab

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.os.ParcelFileDescriptor
import com.jacktor.batterylab.utilities.preferences.PreferencesKeys.BATTERY_LEVEL_TO
import com.jacktor.batterylab.utilities.preferences.PreferencesKeys.BATTERY_LEVEL_WITH
import com.jacktor.batterylab.utilities.preferences.PreferencesKeys.CAPACITY_ADDED
import com.jacktor.batterylab.utilities.preferences.PreferencesKeys.DESIGN_CAPACITY
import com.jacktor.batterylab.utilities.preferences.PreferencesKeys.LAST_CHARGE_TIME
import com.jacktor.batterylab.utilities.preferences.PreferencesKeys.NUMBER_OF_CHARGES
import com.jacktor.batterylab.utilities.preferences.PreferencesKeys.NUMBER_OF_CYCLES
import com.jacktor.batterylab.utilities.preferences.PreferencesKeys.PERCENT_ADDED
import com.jacktor.batterylab.utilities.preferences.PreferencesKeys.RESIDUAL_CAPACITY
import com.jacktor.batterylab.utilities.preferences.Prefs

class ApplicationBackup : BackupAgent() {

    private var pref: Prefs? = null
    private var prefArrays: MutableMap<String, *>? = null

    override fun onCreate() {

        super.onCreate()

        val pref = Prefs(this)

        prefArrays = pref.all()
    }

    override fun onBackup(
        oldState: ParcelFileDescriptor?, data: BackupDataOutput?,
        newState: ParcelFileDescriptor?
    ) {
    }

    override fun onRestore(
        data: BackupDataInput?, appVersionCode: Int,
        newState: ParcelFileDescriptor?
    ) {
    }

    override fun onRestoreFinished() {

        super.onRestoreFinished()

        val prefsTempList = arrayListOf(
            BATTERY_LEVEL_TO, BATTERY_LEVEL_WITH,
            DESIGN_CAPACITY, CAPACITY_ADDED, LAST_CHARGE_TIME, PERCENT_ADDED, RESIDUAL_CAPACITY
        )

        prefsTempList.forEach {
            with(prefArrays) {
                when {

                    this?.containsKey(it) == false -> pref?.remove(it)

                    else -> {

                        this?.forEach {

                            when (it.key) {

                                NUMBER_OF_CHARGES -> pref?.setLong(
                                    it.key,
                                    it.value as Long
                                )

                                BATTERY_LEVEL_TO, BATTERY_LEVEL_WITH, LAST_CHARGE_TIME,
                                DESIGN_CAPACITY, RESIDUAL_CAPACITY, PERCENT_ADDED -> pref
                                    ?.setInt(it.key, it.value as Int)

                                CAPACITY_ADDED, NUMBER_OF_CYCLES -> pref?.setFloat(
                                    it.key,
                                    it.value as Float
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}