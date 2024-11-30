package com.jacktor.batterylab.utilities.preferences

import android.content.SharedPreferences

class PreferenceChangeListener(
    private val onPreferenceChanged: (prefs: SharedPreferences, key: String) -> Unit
) : SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        onPreferenceChanged(sharedPreferences!!, key!!)
    }
}