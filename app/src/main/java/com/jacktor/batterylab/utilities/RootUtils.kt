package com.jacktor.batterylab.utilities

import com.topjohnwu.superuser.Shell

object RootUtils {

    /**
     * Memeriksa apakah perangkat sudah di-root dan akses root tersedia.
     *
     * @return `true` jika perangkat sudah di-root dan akses root tersedia, `false` jika tidak.
     */
    fun hasRootAccess(): Boolean {
        return RootChecker().isDeviceRooted()
    }

    fun reqRootAccess(): Boolean {
        return Shell.cmd("su").exec().isSuccess
    }
}
