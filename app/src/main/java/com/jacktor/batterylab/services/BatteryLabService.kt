package com.jacktor.batterylab.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.os.BatteryManager
import android.os.IBinder
import android.view.Display
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.jacktor.batterylab.MainActivity
import com.jacktor.batterylab.MainApp.Companion.batteryIntent
import com.jacktor.batterylab.MainApp.Companion.isPowerConnected
import com.jacktor.batterylab.R
import com.jacktor.batterylab.adapters.HistoryAdapter
import com.jacktor.batterylab.databases.HistoryDB
import com.jacktor.batterylab.fragments.ChargeDischargeFragment
import com.jacktor.batterylab.fragments.HistoryFragment
import com.jacktor.batterylab.helpers.BatteryLevelHelper
import com.jacktor.batterylab.helpers.DateHelper
import com.jacktor.batterylab.helpers.HistoryHelper
import com.jacktor.batterylab.helpers.ServiceHelper
import com.jacktor.batterylab.interfaces.BatteryInfoInterface
import com.jacktor.batterylab.interfaces.BatteryInfoInterface.Companion.capacityAdded
import com.jacktor.batterylab.interfaces.BatteryInfoInterface.Companion.maxChargeCurrent
import com.jacktor.batterylab.interfaces.BatteryInfoInterface.Companion.percentAdded
import com.jacktor.batterylab.interfaces.BatteryInfoInterface.Companion.tempBatteryLevelWith
import com.jacktor.batterylab.interfaces.BatteryInfoInterface.Companion.tempCurrentCapacity
import com.jacktor.batterylab.interfaces.NotificationInterface
import com.jacktor.batterylab.interfaces.NotificationInterface.Companion.isBatteryCharged
import com.jacktor.batterylab.interfaces.NotificationInterface.Companion.isBatteryDischarged
import com.jacktor.batterylab.interfaces.NotificationInterface.Companion.isBatteryDischargedVoltage
import com.jacktor.batterylab.interfaces.NotificationInterface.Companion.isBatteryFullyCharged
import com.jacktor.batterylab.interfaces.NotificationInterface.Companion.isOverheatOvercool
import com.jacktor.batterylab.interfaces.NotificationInterface.Companion.notificationBuilder
import com.jacktor.batterylab.interfaces.NotificationInterface.Companion.notificationManager
import com.jacktor.batterylab.interfaces.PremiumInterface
import com.jacktor.batterylab.receivers.PluggedReceiver
import com.jacktor.batterylab.receivers.UnpluggedReceiver
import com.jacktor.batterylab.utilities.Constants.CHECK_PREMIUM_JOB_ID
import com.jacktor.batterylab.utilities.Constants.NOMINAL_BATTERY_VOLTAGE
import com.jacktor.batterylab.utilities.Constants.NOTIFY_FULL_CHARGE_REMINDER_JOB_ID
import com.jacktor.batterylab.utilities.PreferencesKeys.BATTERY_LEVEL_NOTIFY_CHARGED
import com.jacktor.batterylab.utilities.PreferencesKeys.BATTERY_LEVEL_NOTIFY_DISCHARGED
import com.jacktor.batterylab.utilities.PreferencesKeys.BATTERY_LEVEL_TO
import com.jacktor.batterylab.utilities.PreferencesKeys.BATTERY_LEVEL_WITH
import com.jacktor.batterylab.utilities.PreferencesKeys.BATTERY_NOTIFY_DISCHARGED_VOLTAGE
import com.jacktor.batterylab.utilities.PreferencesKeys.CAPACITY_ADDED
import com.jacktor.batterylab.utilities.PreferencesKeys.DESIGN_CAPACITY
import com.jacktor.batterylab.utilities.PreferencesKeys.FAST_CHARGE_SETTING
import com.jacktor.batterylab.utilities.PreferencesKeys.FULL_CHARGE_REMINDER_FREQUENCY
import com.jacktor.batterylab.utilities.PreferencesKeys.LAST_CHARGE_TIME
import com.jacktor.batterylab.utilities.PreferencesKeys.NOTIFY_BATTERY_IS_CHARGED
import com.jacktor.batterylab.utilities.PreferencesKeys.NOTIFY_BATTERY_IS_DISCHARGED
import com.jacktor.batterylab.utilities.PreferencesKeys.NOTIFY_BATTERY_IS_DISCHARGED_VOLTAGE
import com.jacktor.batterylab.utilities.PreferencesKeys.NOTIFY_BATTERY_IS_FULLY_CHARGED
import com.jacktor.batterylab.utilities.PreferencesKeys.NOTIFY_OVERHEAT_OVERCOOL
import com.jacktor.batterylab.utilities.PreferencesKeys.NUMBER_OF_CHARGES
import com.jacktor.batterylab.utilities.PreferencesKeys.NUMBER_OF_CYCLES
import com.jacktor.batterylab.utilities.PreferencesKeys.NUMBER_OF_FULL_CHARGES
import com.jacktor.batterylab.utilities.PreferencesKeys.OVERCOOL_DEGREES
import com.jacktor.batterylab.utilities.PreferencesKeys.OVERHEAT_DEGREES
import com.jacktor.batterylab.utilities.PreferencesKeys.PERCENT_ADDED
import com.jacktor.batterylab.utilities.PreferencesKeys.RESIDUAL_CAPACITY
import com.jacktor.batterylab.utilities.PreferencesKeys.UNIT_OF_MEASUREMENT_OF_CURRENT_CAPACITY
import com.jacktor.batterylab.utilities.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class BatteryLabService : Service(), NotificationInterface, BatteryInfoInterface {

    private lateinit var pref: Prefs
    private var screenTimeJob: Job? = null
    private var jobService: Job? = null
    private var isScreenTimeJob = false
    private var isJob = false
    private var currentCapacity = 0

    var isFull = false
    var isStopService = false
    var isSaveNumberOfCharges = true
    var isPluggedOrUnplugged = false
    var batteryLevelWith = -1
    var seconds = 0
    var screenTime = 0L
    var secondsFullCharge = 0

    companion object {

        var instance: BatteryLabService? = null
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onCreate() {

        if (instance == null) {

            super.onCreate()

            instance = this

            pref = Prefs(this)

            batteryIntent = registerReceiver(
                null, IntentFilter(
                    Intent.ACTION_BATTERY_CHANGED
                )
            )

            val numberOfCharges = pref.getLong(NUMBER_OF_CHARGES, 0)

            when (batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {

                BatteryManager.BATTERY_PLUGGED_AC, BatteryManager.BATTERY_PLUGGED_USB,
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> {

                    isPowerConnected = true

                    batteryLevelWith = getBatteryLevel(this) ?: 0

                    tempBatteryLevelWith = batteryLevelWith

                    tempCurrentCapacity = getCurrentCapacity(this)

                    val status = batteryIntent?.getIntExtra(
                        BatteryManager.EXTRA_STATUS,
                        BatteryManager.BATTERY_STATUS_UNKNOWN
                    ) ?: BatteryManager
                        .BATTERY_STATUS_UNKNOWN

                    pref.setLong(NUMBER_OF_CHARGES, numberOfCharges + 1)

                    if (MainActivity.instance?.fragment != null) {

                        if (MainActivity.instance?.fragment is ChargeDischargeFragment)
                            MainActivity.instance?.topAppBar?.title = getString(
                                if (status ==
                                    BatteryManager.BATTERY_STATUS_CHARGING
                                ) R.string.charge else
                                    R.string.discharge
                            )

                        val chargeDischargeNavigation = MainActivity.instance?.navigation
                            ?.menu?.findItem(R.id.charge_discharge_navigation)

                        chargeDischargeNavigation?.title = getString(
                            if (status == BatteryManager
                                    .BATTERY_STATUS_CHARGING
                            ) R.string.charge else R.string.discharge
                        )

                        chargeDischargeNavigation?.icon = BatteryLevelHelper.batteryLevelIcon(
                            getBatteryLevel(this), status == BatteryManager
                                .BATTERY_STATUS_CHARGING
                        ).let {
                            ContextCompat.getDrawable(this, it)
                        }
                    }


                }
            }

            applicationContext.registerReceiver(
                PluggedReceiver(), IntentFilter(
                    Intent.ACTION_POWER_CONNECTED
                )
            )

            applicationContext.registerReceiver(
                UnpluggedReceiver(), IntentFilter(
                    Intent.ACTION_POWER_DISCONNECTED
                )
            )

            onCreateServiceNotification(this@BatteryLabService)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (screenTimeJob == null)
            screenTimeJob = CoroutineScope(Dispatchers.Default).launch {

                isScreenTimeJob = !isScreenTimeJob

                while (isScreenTimeJob && !isStopService) {

                    val status = batteryIntent?.getIntExtra(
                        BatteryManager.EXTRA_STATUS,
                        BatteryManager.BATTERY_STATUS_UNKNOWN
                    )

                    if ((status == BatteryManager.BATTERY_STATUS_DISCHARGING ||
                                status == BatteryManager.BATTERY_STATUS_NOT_CHARGING)
                        && !isPowerConnected
                    ) {

                        val displayManager = getSystemService(Context.DISPLAY_SERVICE)
                                as? DisplayManager

                        if (displayManager != null)
                            display@ for (display in displayManager.displays)
                                if (display.state == Display.STATE_ON) {
                                    screenTime++
                                    break@display
                                }
                    }

                    delay(1000L)
                }
            }

        if (jobService == null)
            jobService = CoroutineScope(Dispatchers.Default).launch {

                isJob = !isJob

                while (isJob && !isStopService) {

                    if (instance == null) instance = this@BatteryLabService

                    if ((getBatteryLevel(this@BatteryLabService) ?: 0) < batteryLevelWith)
                        batteryLevelWith = getBatteryLevel(this@BatteryLabService) ?: 0

                    batteryIntent = registerReceiver(
                        null, IntentFilter(
                            Intent.ACTION_BATTERY_CHANGED
                        )
                    )

                    val status = batteryIntent?.getIntExtra(
                        BatteryManager.EXTRA_STATUS,
                        BatteryManager.BATTERY_STATUS_UNKNOWN
                    )

                    val temperature = getTemperatureInCelsius(this@BatteryLabService)

                    if (!isPluggedOrUnplugged) {

                        BatteryInfoInterface.maximumTemperature =
                            getMaximumTemperature(
                                this@BatteryLabService,
                                BatteryInfoInterface.maximumTemperature
                            )

                        BatteryInfoInterface.minimumTemperature =
                            getMinimumTemperature(
                                this@BatteryLabService,
                                BatteryInfoInterface.minimumTemperature
                            )

                        BatteryInfoInterface.averageTemperature = getAverageTemperature(
                            this@BatteryLabService,
                            BatteryInfoInterface.maximumTemperature,
                            BatteryInfoInterface.minimumTemperature
                        )
                    }

                    if (pref.getBoolean(
                            NOTIFY_OVERHEAT_OVERCOOL, resources.getBoolean(
                                R.bool.notify_overheat_overcool
                            )
                        ) && (temperature >= pref
                            .getInt(
                                OVERHEAT_DEGREES, resources.getInteger(
                                    R.integer
                                        .overheat_degrees_default
                                )
                            ) ||
                                temperature <= pref.getInt(
                            OVERCOOL_DEGREES, resources.getInteger(
                                R.integer.overcool_degrees_default
                            )
                        ))
                    )
                        withContext(Dispatchers.Main) {
                            onNotifyOverheatOvercool(this@BatteryLabService, temperature)
                        }

                    if (status == BatteryManager.BATTERY_STATUS_CHARGING
                        && !isStopService && secondsFullCharge < 3600
                    ) batteryCharging()
                    else if (status == BatteryManager.BATTERY_STATUS_FULL && isPowerConnected &&
                        !isFull && !isStopService
                    ) batteryCharged()
                    else if (!isStopService) {

                        if (pref.getBoolean(
                                NOTIFY_BATTERY_IS_DISCHARGED, resources.getBoolean(
                                    R.bool.notify_battery_is_discharged
                                )
                            ) && (getBatteryLevel(
                                this@BatteryLabService
                            ) ?: 0) <= pref.getInt(
                                BATTERY_LEVEL_NOTIFY_DISCHARGED, 20
                            )
                        )
                            withContext(Dispatchers.Main) {

                                onNotifyBatteryDischarged(this@BatteryLabService)
                            }

                        if (pref.getBoolean(
                                NOTIFY_BATTERY_IS_DISCHARGED_VOLTAGE,
                                resources.getBoolean(R.bool.notify_battery_is_discharged_voltage)
                            )
                        ) {

                            val voltage = getVoltage(this@BatteryLabService)

                            if (voltage <= pref.getInt(
                                    BATTERY_NOTIFY_DISCHARGED_VOLTAGE, resources
                                        .getInteger(R.integer.battery_notify_discharged_voltage_min)
                                )
                            )
                                withContext(Dispatchers.Main) {
                                    onNotifyBatteryDischargedVoltage(
                                        this@BatteryLabService, voltage.toInt()
                                    )
                                }
                        }

                        withContext(Dispatchers.Main) {
                            onUpdateServiceNotification(this@BatteryLabService)
                        }

                        delay(1496L)
                    }
                }
            }

        return START_STICKY
    }

    override fun onDestroy() {

        instance = null
        isScreenTimeJob = false
        isJob = false
        screenTimeJob?.cancel()
        jobService?.cancel()
        screenTimeJob = null
        jobService = null
        notificationBuilder = null

        isOverheatOvercool = false
        isBatteryFullyCharged = false
        isBatteryCharged = false
        isBatteryDischarged = false
        isBatteryDischargedVoltage = false

        val batteryLevel = getBatteryLevel(this) ?: 0

        val numberOfCycles = if (batteryLevel == batteryLevelWith) pref.getFloat(
            NUMBER_OF_CYCLES, 0f
        ) + 0.01f else pref.getFloat(
            NUMBER_OF_CYCLES, 0f
        ) + (batteryLevel / 100f) - (
                batteryLevelWith / 100f)

        notificationManager?.cancelAll()

        if (!::pref.isInitialized) pref = Prefs(this)

        if (!isFull && seconds > 1) {

            pref.apply {

                setInt(LAST_CHARGE_TIME, seconds)

                setInt(BATTERY_LEVEL_WITH, batteryLevelWith)

                setInt(BATTERY_LEVEL_TO, batteryLevel)

                if (capacityAdded > 0) setFloat(CAPACITY_ADDED, capacityAdded.toFloat())

                if (percentAdded > 0) setInt(PERCENT_ADDED, percentAdded)

                if (isSaveNumberOfCharges) setFloat(NUMBER_OF_CYCLES, numberOfCycles)
            }

            percentAdded = 0

            capacityAdded = 0.0
        }

        if (BatteryInfoInterface.residualCapacity > 0 && isFull) {

            pref.apply {

                if (pref.getString(UNIT_OF_MEASUREMENT_OF_CURRENT_CAPACITY, "μAh")
                    == "μAh"
                )
                    setInt(
                        RESIDUAL_CAPACITY,
                        (getCurrentCapacity(applicationContext) * 1000.0).toInt()
                    )
                else setInt(
                    RESIDUAL_CAPACITY,
                    (getCurrentCapacity(applicationContext) * 100.0).toInt()
                )
            }

            HistoryHelper.addHistory(
                this, DateHelper.getDate(
                    DateHelper.getCurrentDay(),
                    DateHelper.getCurrentMonth(), DateHelper.getCurrentYear()
                ), pref.getInt(
                    RESIDUAL_CAPACITY, 0
                )
            )
        }

        BatteryInfoInterface.batteryLevel = 0
        BatteryInfoInterface.tempBatteryLevel = 0

        if (isStopService)
            Toast.makeText(
                this, R.string.service_stopped_successfully,
                Toast.LENGTH_LONG
            ).show()

        ServiceHelper.cancelJob(this, NOTIFY_FULL_CHARGE_REMINDER_JOB_ID)
        ServiceHelper.cancelJob(this, CHECK_PREMIUM_JOB_ID)

        super.onDestroy()
    }

    private suspend fun batteryCharging() {

        val batteryLevel = getBatteryLevel(this@BatteryLabService) ?: 0

        if (batteryLevel == 100) {
            if (secondsFullCharge >= 3600) batteryCharged()
            currentCapacity = (getCurrentCapacity(this) * if (pref.getString(
                    UNIT_OF_MEASUREMENT_OF_CURRENT_CAPACITY, "μAh"
                ) == "μAh"
            )
                1000.0 else 100.0).toInt()
            secondsFullCharge++
        }

        val displayManager = getSystemService(Context.DISPLAY_SERVICE)
                as? DisplayManager

        if (pref.getBoolean(
                NOTIFY_BATTERY_IS_CHARGED, resources.getBoolean(
                    R.bool.notify_battery_is_charged
                )
            ) &&
            (getBatteryLevel(this) ?: 0) == pref.getInt(
                BATTERY_LEVEL_NOTIFY_CHARGED,
                80
            )
        )
            withContext(Dispatchers.Main) {

                onNotifyBatteryCharged(this@BatteryLabService)
            }

        if (displayManager != null)
            for (display in displayManager.displays)
                if (display.state == Display.STATE_ON)
                    delay(
                        if (getCurrentCapacity(this@BatteryLabService) > 0.0) 0.949.seconds else 0.955.seconds
                    )
                else delay(
                    if (getCurrentCapacity(this@BatteryLabService) > 0.0) 0.938.seconds else 0.935.seconds
                )

        seconds++

        try {

            withContext(Dispatchers.Main) {
                onUpdateServiceNotification(this@BatteryLabService)
            }
        } catch (_: RuntimeException) {
        }
    }

    private suspend fun batteryCharged() {

        withContext(Dispatchers.Main) {
            val fullChargeReminderFrequency = pref.getString(
                FULL_CHARGE_REMINDER_FREQUENCY,
                "${resources.getInteger(R.integer.full_charge_reminder_frequency_default)}"
            )?.toInt()

            ServiceHelper.jobSchedule(
                this@BatteryLabService,
                FullChargeReminderJobService::class.java, NOTIFY_FULL_CHARGE_REMINDER_JOB_ID,
                fullChargeReminderFrequency?.minutes?.inWholeMilliseconds ?: resources
                    .getInteger(R.integer.full_charge_reminder_frequency_default).minutes
                    .inWholeMilliseconds
            )
        }

        isFull = true

        if (currentCapacity == 0)
            currentCapacity = (getCurrentCapacity(this) * if (pref.getString(
                    UNIT_OF_MEASUREMENT_OF_CURRENT_CAPACITY, "μAh"
                ) == "μAh"
            ) 1000.0
            else 100.0).toInt()

        val designCapacity = pref.getInt(
            DESIGN_CAPACITY, resources.getInteger(
                R.integer.min_design_capacity
            )
        ).toDouble() * if (pref.getString(
                UNIT_OF_MEASUREMENT_OF_CURRENT_CAPACITY, "μAh"
            ) == "μAh"
        ) 1000.0
        else 100.0

        val residualCapacityCurrent = if (pref.getString(
                UNIT_OF_MEASUREMENT_OF_CURRENT_CAPACITY,
                "μAh"
            ) == "μAh"
        ) pref.getInt(RESIDUAL_CAPACITY, 0) / 1000
        else pref.getInt(RESIDUAL_CAPACITY, 0) / 100

        val residualCapacity =
            if (residualCapacityCurrent in 1..maxChargeCurrent ||
                isTurboCharge(this@BatteryLabService) || pref.getBoolean(
                    FAST_CHARGE_SETTING, resources.getBoolean(R.bool.fast_charge_setting)
                )
            )
                (currentCapacity.toDouble() +
                        ((NOMINAL_BATTERY_VOLTAGE / 100.0) * designCapacity)).toInt()
            else currentCapacity

        val currentDate = DateHelper.getDate(
            DateHelper.getCurrentDay(),
            DateHelper.getCurrentMonth(), DateHelper.getCurrentYear()
        )

        if (pref.getBoolean(
                NOTIFY_BATTERY_IS_FULLY_CHARGED, resources.getBoolean(
                    R.bool.notify_battery_is_fully_charged
                )
            )
        )
            withContext(Dispatchers.Main) {

                onNotifyBatteryFullyCharged(this@BatteryLabService)
            }

        val batteryLevel = getBatteryLevel(this@BatteryLabService) ?: 0

        val numberOfCycles = if (batteryLevel == batteryLevelWith) pref.getFloat(
            NUMBER_OF_CYCLES, 0f
        ) + 0.01f else pref.getFloat(
            NUMBER_OF_CYCLES, 0f
        ) + (batteryLevel / 100f) - (
                batteryLevelWith / 100f)

        pref.apply {

            setInt(LAST_CHARGE_TIME, seconds)
            setInt(RESIDUAL_CAPACITY, residualCapacity)
            setInt(BATTERY_LEVEL_WITH, batteryLevelWith)
            setInt(BATTERY_LEVEL_TO, batteryLevel)
            setLong(NUMBER_OF_FULL_CHARGES, pref.getLong(NUMBER_OF_FULL_CHARGES, 0) + 1)
            setFloat(CAPACITY_ADDED, capacityAdded.toFloat())
            setInt(PERCENT_ADDED, percentAdded)

            if (isSaveNumberOfCharges) setFloat(NUMBER_OF_CYCLES, numberOfCycles)
        }

        withContext(Dispatchers.Main) {
            if (PremiumInterface.isPremium) {
                if (residualCapacity > 0 && seconds >= 10) {
                    withContext(Dispatchers.IO) {
                        HistoryHelper.addHistory(this@BatteryLabService, currentDate,
                            residualCapacity)
                    }
                    if (HistoryHelper.isHistoryNotEmpty(this@BatteryLabService)) {
                        val historyFragment = HistoryFragment.instance
                        historyFragment?.binding?.refreshEmptyHistory?.visibility = View.GONE
                        historyFragment?.binding?.emptyHistoryLayout?.visibility = View.GONE
                        historyFragment?.binding?.historyRecyclerView?.visibility = View.VISIBLE
                        historyFragment?.binding?.refreshHistory?.visibility = View.VISIBLE

                        MainActivity.instance?.topAppBar?.menu?.findItem(R.id.history_premium)
                            ?.isVisible = false
                        MainActivity.instance?.topAppBar?.menu?.findItem(R.id.clear_history)
                            ?.isVisible = true

                        if(HistoryHelper.getHistoryCount(this@BatteryLabService) == 1L) {
                            val historyDB = withContext(Dispatchers.IO) {
                                HistoryDB(this@BatteryLabService)
                            }
                            historyFragment?.historyAdapter =
                                HistoryAdapter(withContext(Dispatchers.IO) {
                                        historyDB.readDB()
                            })
                            historyFragment?.historyAdapter?.itemCount?.let {
                                historyFragment.binding?.historyRecyclerView?.setItemViewCacheSize(
                                    it
                                )
                            }
                            historyFragment?.binding?.historyRecyclerView?.adapter =
                                historyFragment?.historyAdapter
                        }

                        else HistoryAdapter.instance?.update(this@BatteryLabService)

                    } else {
                        HistoryFragment.instance?.binding?.historyRecyclerView?.visibility =
                            View.GONE
                        HistoryFragment.instance?.binding?.refreshHistory?.visibility = View.GONE
                        HistoryFragment.instance?.binding?.emptyHistoryLayout?.visibility =
                            View.VISIBLE
                        HistoryFragment.instance?.binding?.refreshEmptyHistory?.visibility =
                            View.VISIBLE
                        HistoryFragment.instance?.binding?.emptyHistoryText?.text =
                            resources.getText(R.string.empty_history_text)
                        MainActivity.instance?.topAppBar?.menu?.findItem(R.id.history_premium)
                            ?.isVisible = false
                        MainActivity.instance?.topAppBar?.menu?.findItem(R.id.clear_history)
                            ?.isVisible = false
                    }
                } else {
                    HistoryFragment.instance?.binding?.historyRecyclerView?.visibility = View.GONE
                    HistoryFragment.instance?.binding?.refreshHistory?.visibility = View.GONE
                    HistoryFragment.instance?.binding?.emptyHistoryLayout?.visibility = View.VISIBLE
                    HistoryFragment.instance?.binding?.refreshEmptyHistory?.visibility = View.VISIBLE
                    HistoryFragment.instance?.binding?.emptyHistoryText?.text =
                        resources.getText(R.string.history_premium_feature)
                    MainActivity.instance?.topAppBar?.menu?.findItem(R.id.history_premium)
                        ?.isVisible = true
                    MainActivity.instance?.topAppBar?.menu?.findItem(R.id.clear_history)?.isVisible =
                        false
                }
            }
        }

        isSaveNumberOfCharges = false

        withContext(Dispatchers.Main) {
            onUpdateServiceNotification(this@BatteryLabService)
        }
    }
}