package com.jacktor.rootchecker

internal class Const private constructor() {
    init {
        throw InstantiationException("This class is not for instantiation")
    }

    companion object {
        const val BINARY_SU = "su"
        const val BINARY_BUSYBOX = "busybox"
        val knownRootAppsPackages = arrayOf(
            "com.noshufou.android.su",
            "com.noshufou.android.su.elite",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.thirdparty.superuser",
            "com.yellowes.su",
            "com.topjohnwu.magisk",
            "com.kingroot.kinguser",
            "com.kingo.root",
            "com.smedialink.oneclickroot",
            "com.zhiqupk.root.global",
            "com.alephzain.framaroot"
        )
        val knownDangerousAppsPackages = arrayOf(
            "com.koushikdutta.rommanager",
            "com.koushikdutta.rommanager.license",
            "com.dimonvideo.luckypatcher",
            "com.chelpus.lackypatch",
            "com.ramdroid.appquarantine",
            "com.ramdroid.appquarantinepro",
            "com.android.vending.billing.InAppBillingService.COIN",
            "com.android.vending.billing.InAppBillingService.LUCK",
            "com.chelpus.luckypatcher",
            "com.blackmartalpha",
            "org.blackmart.market",
            "com.allinone.free",
            "com.repodroid.app",
            "org.creeplays.hack",
            "com.baseappfull.fwd",
            "com.zmapp",
            "com.dv.marketmod.installer",
            "org.mobilism.android",
            "com.android.wp.net.log",
            "com.android.camera.update",
            "cc.madkite.freedom",
            "com.solohsu.android.edxp.manager",
            "org.meowcat.edxposed.manager",
            "com.xmodgame",
            "com.cih.game_cih",
            "com.charles.lpoqasert",
            "catch_.me_.if_.you_.can_"
        )
        val knownRootCloakingPackages = arrayOf(
            "com.devadvance.rootcloak",
            "com.devadvance.rootcloakplus",
            "de.robv.android.xposed.installer",
            "com.saurik.substrate",
            "com.zachspong.temprootremovejb",
            "com.amphoras.hidemyroot",
            "com.amphoras.hidemyrootadfree",
            "com.formyhm.hiderootPremium",
            "com.formyhm.hideroot"
        )

        // These must end with a /
        private val suPaths = arrayOf(
            "/data/local/",
            "/data/local/bin/",
            "/data/local/xbin/",
            "/sbin/",
            "/su/bin/",
            "/system/bin/",
            "/system/bin/.ext/",
            "/system/bin/failsafe/",
            "/system/sd/xbin/",
            "/system/usr/we-need-root/",
            "/system/xbin/",
            "/cache/",
            "/data/",
            "/dev/"
        )
        val pathsThatShouldNotBeWritable = arrayOf(
            "/system",
            "/system/bin",
            "/system/sbin",
            "/system/xbin",
            "/vendor/bin",
            "/sbin",
            "/etc"
        )
        @Suppress("unused")
        val paths: Array<String>
            /**
             * Get a list of paths to check for binaries
             *
             * @return List of paths to check, using a combination of a static list and those paths
             * listed in the PATH environment variable.
             */
            get() {
                val paths = ArrayList(listOf(*suPaths))
                val sysPaths = System.getenv("PATH")

                // If we can't get the path variable just return the static paths
                if (sysPaths == null || "" == sysPaths) {
                    return paths.toTypedArray()
                }
                for (path in sysPaths.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()) {
                    var path1 = path
                    if (!path1.endsWith("/")) {
                        path1 = "$path1/"
                    }
                    if (!paths.contains(path1)) {
                        paths.add(path1)
                    }
                }
                return paths.toTypedArray()
            }
    }
}