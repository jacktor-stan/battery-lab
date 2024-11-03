package com.jacktor.batterylab.utilities

import android.content.Context
import com.jacktor.rootchecker.RootChecker
import com.topjohnwu.superuser.Shell

object RootUtils {

    /**
     * Memeriksa apakah perangkat sudah di-root dan akses root tersedia.
     *
     * @param context Konteks aplikasi.
     * @return `true` jika perangkat sudah di-root dan akses root tersedia, `false` jika tidak.
     */
    fun hasRootAccess(context: Context): Boolean {
        val rootChecker = RootChecker(context)
        return rootChecker.isRooted
    }

    fun reqRootAccess(): Boolean {
        return Shell.cmd("su").exec().isSuccess
    }
}
