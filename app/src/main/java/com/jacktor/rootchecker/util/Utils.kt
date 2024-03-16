@file:Suppress("unused")

package com.jacktor.rootchecker.util

import android.annotation.SuppressLint

class Utils private constructor() {
    init {
        throw InstantiationException("This class is not for instantiation")
    }

    companion object {
        val isSelinuxFlagInEnabled: Boolean
            /**
             * In Development - an idea of ours was to check the if selinux is enforcing - this could be disabled for some rooting apps
             * Checking for selinux mode
             *
             * @return true if selinux enabled
             */
            @SuppressLint("PrivateApi")
            get() {
                try {
                    val c = Class.forName("android.os.SystemProperties")
                    val get = c.getMethod("get", String::class.java)
                    val selinux = get.invoke(c, "ro.build.selinux") as String
                    return "1" == selinux
                } catch (ignored: Exception) {
                }
                return false
            }
    }
}