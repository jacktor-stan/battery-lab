package com.jacktor.batterylab.utilities.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class Prefs(context: Context) {
    private val sharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)
    private val editor = sharedPreferences.edit()

    // Mendapatkan semua key-value
    fun all(): MutableMap<String, *>? {
        return sharedPreferences.all
    }

    // Mengecek keberadaan key
    fun contains(key: String): Boolean {
        return sharedPreferences.contains(key)
    }

    // Menghapus key
    fun remove(key: String?) {
        editor.remove(key)
        editor.apply()
    }

    // Setter untuk berbagai tipe data
    fun setInt(key: String?, value: Int) {
        editor.putInt(key, value)
        editor.apply()
    }

    fun setString(key: String?, value: String?) {
        editor.putString(key, value)
        editor.apply()
    }

    fun setBoolean(key: String?, value: Boolean) {
        editor.putBoolean(key, value)
        editor.apply()
    }

    fun setLong(key: String?, value: Long) {
        editor.putLong(key, value)
        editor.apply()
    }

    fun setFloat(key: String?, value: Float) {
        editor.putFloat(key, value)
        editor.apply()
    }

    // Getter untuk berbagai tipe data
    fun getBoolean(key: String?, def: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, def)
    }

    fun getInt(key: String?, def: Int): Int {
        return sharedPreferences.getInt(key, def)
    }

    fun getString(key: String?, def: String?): String? {
        return sharedPreferences.getString(key, def)
    }

    fun getFloat(key: String, def: Float): Float {
        return sharedPreferences.getFloat(key, def)
    }

    fun getLong(key: String, def: Long): Long {
        return sharedPreferences.getLong(key, def)
    }

    // Menambahkan listener
    fun addOnPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }

    // Menghapus listener
    fun removeOnPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
    }
}