package com.jacktor.batterylab.utilities

import android.content.Context
import android.os.Environment
import java.io.File

object SDUtil {
    //private val EXTERNAL_STORAGE: String? = System.getenv("EXTERNAL_STORAGE")
    private val SECONDARY_STORAGES: String? = System.getenv("SECONDARY_STORAGE")
    private val EMULATED_STORAGE_TARGET: String? = System.getenv("EMULATED_STORAGE_TARGET")

    fun getStorageDirectories(context: Context): Array<String> {
        val availableDirectoriesSet = mutableSetOf<String>()

        if (!EMULATED_STORAGE_TARGET.isNullOrEmpty()) {
            availableDirectoriesSet.add(getEmulatedStorageTarget())
        } else {
            availableDirectoriesSet.addAll(getExternalStorage(context))
        }

        availableDirectoriesSet.addAll(getAllSecondaryStorages())
        return availableDirectoriesSet.toTypedArray()
    }

    private fun getExternalStorage(context: Context): Set<String> {
        val availableDirectoriesSet = mutableSetOf<String>()
        val files = getExternalFilesDirs(context)
        for (file in files) {
            file?.let {
                val applicationSpecificAbsolutePath = it.absolutePath
                var rootPath = applicationSpecificAbsolutePath.substring(
                    9,
                    applicationSpecificAbsolutePath.indexOf("Android/data")
                )
                rootPath = rootPath.substring(rootPath.indexOf("/storage/") + 1)
                rootPath = rootPath.substring(0, rootPath.indexOf("/"))

                if (rootPath != "emulated") {
                    availableDirectoriesSet.add(rootPath)
                }
            }
        }
        return availableDirectoriesSet
    }

    private fun getEmulatedStorageTarget(): String {
        val path = Environment.getExternalStorageDirectory().absolutePath
        val folders = path.split(File.separator)
        val lastSegment = folders.lastOrNull()
        val rawStorageId = if (!lastSegment.isNullOrEmpty() && lastSegment.all { it.isDigit() }) {
            lastSegment
        } else {
            ""
        }

        return if (rawStorageId.isEmpty()) {
            EMULATED_STORAGE_TARGET ?: ""
        } else {
            "$EMULATED_STORAGE_TARGET${File.separator}$rawStorageId"
        }
    }

    private fun getAllSecondaryStorages(): List<String> {
        return SECONDARY_STORAGES?.split(File.pathSeparator) ?: emptyList()
    }

    /*@SuppressLint("SdCardPath")
    private val KNOWN_PHYSICAL_PATHS = listOf(
        "/storage/sdcard0",
        "/storage/sdcard1",
        "/storage/extsdcard",
        "/storage/sdcard0/external_sdcard",
        "/mnt/extsdcard",
        "/mnt/sdcard/external_sd",
        "/mnt/sdcard/ext_sd",
        "/mnt/external_sd",
        "/mnt/media_rw/sdcard1",
        "/removable/microsd",
        "/mnt/emmc",
        "/storage/external_SD",
        "/storage/ext_sd",
        "/storage/removable/sdcard1",
        "/data/sdext",
        "/data/sdext2",
        "/data/sdext3",
        "/data/sdext4",
        "/sdcard1",
        "/sdcard2",
        "/storage/microsd"
    )*/

    private fun getExternalFilesDirs(context: Context): Array<File?> {
        return context.getExternalFilesDirs(null)
    }
}
