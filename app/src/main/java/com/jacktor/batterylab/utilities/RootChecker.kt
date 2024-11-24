package com.jacktor.batterylab.utilities

class RootChecker {
        // Load the native library
        init {
            System.loadLibrary("native-jacktor")
        }

        // Declare the native method
        external fun isDeviceRooted(): Boolean
}