package com.jacktor.batterylab.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import com.jacktor.batterylab.R

class StopBatteryLabService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val batteryLabService = BatteryLabService.instance

        batteryLabService?.isStopService = true

        Toast.makeText(this, R.string.stopping_service, Toast.LENGTH_LONG).show()

        stopService(Intent(this, BatteryLabService::class.java))

        stopService(Intent(this, StopBatteryLabService::class.java))

        return START_NOT_STICKY
    }
}