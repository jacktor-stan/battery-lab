package com.jacktor.rootchecker

import com.jacktor.rootchecker.util.QLog

class RootCheckerNative {
    fun wasNativeLibraryLoaded(): Boolean {
        return libraryLoaded
    }

    external fun checkForRoot(pathArray: Array<String?>?): Int
    external fun setLogDebugMessages(logDebugMessages: Boolean): Int

    companion object {
        private var libraryLoaded = false

        /*
     * Loads the C/C++ libraries statically
     */
        init {
            try {
                System.loadLibrary("toolChecker")
                libraryLoaded = true
            } catch (e: UnsatisfiedLinkError) {
                QLog.e(e)
            }
        }
    }
}