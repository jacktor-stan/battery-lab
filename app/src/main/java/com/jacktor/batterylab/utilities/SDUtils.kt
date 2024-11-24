package com.jacktor.batterylab.utilities

import android.content.Context
import android.os.Environment
import java.io.File

object SDUtils {

    private val secondaryStorages: String? = System.getenv("SECONDARY_STORAGE")
    private val emulatedStorageTarget: String? = System.getenv("EMULATED_STORAGE_TARGET")

    /**
     * Mengembalikan array direktori penyimpanan yang tersedia.
     */
    fun getStorageDirectories(context: Context): Array<String> {
        val storageDirectories = mutableSetOf<String>()

        emulatedStorageTarget?.let {
            storageDirectories.add(getEmulatedStoragePath())
        } ?: run {
            storageDirectories.addAll(getExternalStoragePaths(context))
        }

        storageDirectories.addAll(getSecondaryStoragePaths())
        return storageDirectories.toTypedArray()
    }

    /**
     * Mendapatkan daftar direktori penyimpanan eksternal berdasarkan path aplikasi.
     */
    private fun getExternalStoragePaths(context: Context): Set<String> {
        return context.getExternalFilesDirs(null)
            .filterNotNull()
            .mapNotNull { file ->
                extractRootPath(file.absolutePath)
            }
            .toSet()
    }

    /**
     * Mengembalikan path emulated storage.
     */
    private fun getEmulatedStoragePath(): String {
        val storageDirectory = Environment.getExternalStorageDirectory().absolutePath
        val storageId = storageDirectory.split(File.separator).lastOrNull()
            ?.takeIf { it.all { c -> c.isDigit() } }
        return if (storageId.isNullOrEmpty()) {
            emulatedStorageTarget ?: ""
        } else {
            "$emulatedStorageTarget${File.separator}$storageId"
        }
    }

    /**
     * Mengembalikan path penyimpanan sekunder.
     */
    private fun getSecondaryStoragePaths(): List<String> {
        return secondaryStorages?.split(File.pathSeparator) ?: emptyList()
    }

    /**
     * Mengekstrak root path dari direktori aplikasi.
     */
    private fun extractRootPath(absolutePath: String): String? {
        val startIndex = absolutePath.indexOf("/storage/")
        val endIndex = absolutePath.indexOf("/Android/data")

        if (startIndex == -1 || endIndex == -1) return null

        val rootPath = absolutePath.substring(startIndex + 9, endIndex)
        return if (rootPath == "emulated") null else rootPath
    }
}
