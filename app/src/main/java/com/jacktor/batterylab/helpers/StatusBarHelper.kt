package com.jacktor.batterylab.helpers

import com.jacktor.batterylab.R

object StatusBarHelper {
    fun stat(level: Int?): Int {
        return when {
            level == null || level < 0 || level > 100 -> R.drawable.ic_battery_stat_24
            else -> {
                val resourceName = if (level == 100) {
                    "hundred"
                } else {
                    "${toWord(level / 10)}_${toWord(level % 10)}"
                }
                val resId = R.mipmap::class.java.getDeclaredField(resourceName).getInt(null)
                resId
            }
        }
    }

    // Fungsi untuk mengubah angka ke dalam teks
    private fun toWord(digit: Int): String {
        return when (digit) {
            0 -> "zero"
            1 -> "one"
            2 -> "two"
            3 -> "three"
            4 -> "four"
            5 -> "five"
            6 -> "six"
            7 -> "seven"
            8 -> "eight"
            9 -> "nine"
            else -> throw IllegalArgumentException("Invalid digit: $digit")
        }
    }
}