package com.jacktor.batterylab.services

import android.app.job.JobParameters
import android.app.job.JobService
import androidx.work.Configuration
import com.jacktor.batterylab.MainApp
import com.jacktor.batterylab.interfaces.PremiumInterface

class CheckPremiumJob : JobService(), PremiumInterface {

    companion object {
        var isCheckPremiumJob = false
    }

    override fun onStartJob(p0: JobParameters?): Boolean {
        if(isCheckPremiumJob && MainApp.isInstalledGooglePlay) checkPremiumJob()
        else isCheckPremiumJob = true
        return false
    }

    override fun onStopJob(p0: JobParameters?) = true

    init {
        Configuration.Builder().setJobSchedulerJobIdRange(0, 1000).build()
    }
}

