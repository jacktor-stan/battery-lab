package com.jacktor.batterylab.utilities

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.File

object FileUtils {
    private var failReason: String? = null

    fun getRealPath(context: Context, uri: Uri): String? {
        return when {
            DocumentsContract.isDocumentUri(context, uri) -> handleDocumentUri(context, uri)
            uri.scheme.equals("content", ignoreCase = true) -> handleContentUri(context, uri)
            uri.scheme.equals("file", ignoreCase = true) -> uri.path
            else -> null
        }
    }

    private fun handleDocumentUri(context: Context, uri: Uri): String? {
        return when {
            isExternalStorageDocument(uri) -> handleExternalStorageDocument(context, uri)
            isRawDownloadsDocument(uri) -> handleRawDownloadsDocument(context, uri)
            isDownloadsDocument(uri) -> handleDownloadsDocument(context, uri)
            isMediaDocument(uri) -> handleMediaDocument(context, uri)
            else -> null
        }
    }

    private fun handleExternalStorageDocument(context: Context, uri: Uri): String? {
        val docId = DocumentsContract.getDocumentId(uri)
        val split = docId.split(":")
        val type = split[0]

        return when {
            "primary".equals(type, ignoreCase = true) -> {
                Environment.getExternalStorageDirectory().toString() + "/" + (split.getOrNull(1)
                    ?: "")
            }

            else -> getExternalStoragePath(context, split[1], type)
        }
    }

    private fun getExternalStoragePath(
        context: Context,
        relativePath: String,
        type: String
    ): String? {
        val availableStorages = SDUtils.getStorageDirectories(context)
        return availableStorages
            .map { if (relativePath.startsWith("/")) it + relativePath else "$it/$relativePath" }
            .firstOrNull { it.contains(type) || File(it).exists() }
    }

    private fun handleRawDownloadsDocument(context: Context, uri: Uri): String? {
        val fileName = getFilePath(context, uri)
        val subFolder = getSubFolders(uri)
        return fileName?.let {
            Environment.getExternalStorageDirectory().toString() + "/Download/$subFolder$fileName"
        }
    }

    private fun handleDownloadsDocument(context: Context, uri: Uri): String? {
        val id = DocumentsContract.getDocumentId(uri).let {
            when {
                it.startsWith("raw:") -> it.removePrefix("raw:")
                it.startsWith("raw%3A%2F") -> it.removePrefix("raw%3A%2F")
                else -> it
            }
        }

        if (File(id).exists()) return id

        val contentUri = ContentUris.withAppendedId(
            Uri.parse("content://downloads/public_downloads"), id.toLongOrNull() ?: return null
        )
        return getDataColumn(context, contentUri, null, null)
    }

    private fun handleMediaDocument(context: Context, uri: Uri): String? {
        val docId = DocumentsContract.getDocumentId(uri)
        val split = docId.split(":")
        val type = split[0]
        val contentUri = when (type) {
            "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else -> null
        }
        return getDataColumn(context, contentUri, "_id=?", arrayOf(split[1]))
    }

    private fun handleContentUri(context: Context, uri: Uri): String? {
        if (isGooglePhotosUri(uri)) return uri.lastPathSegment
        return getDataColumn(context, uri, null, null).also {
            if (it == null) failReason = "dataReturnedNull"
        }
    }

    private fun getDataColumn(
        context: Context, uri: Uri?, selection: String?, selectionArgs: Array<String>?
    ): String? {
        var cursor: Cursor? = null
        return try {
            val projection = arrayOf("_data")
            cursor =
                context.contentResolver.query(uri!!, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                cursor.getString(cursor.getColumnIndexOrThrow("_data"))
            } else null
        } catch (e: Exception) {
            failReason = e.message
            null
        } finally {
            cursor?.close()
        }
    }

    private fun getFilePath(context: Context, uri: Uri?): String? {
        var cursor: Cursor? = null
        return try {
            val projection = arrayOf(MediaStore.Files.FileColumns.DISPLAY_NAME)
            cursor = context.contentResolver.query(uri!!, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME))
            } else null
        } catch (e: Exception) {
            failReason = e.message
            null
        } finally {
            cursor?.close()
        }
    }

    private fun getSubFolders(uri: Uri): String {
        val pathSegments = uri.toString()
            .replace("%2F", "/").replace("%20", " ").replace("%3A", ":")
            .split("/")
        val downloadIndex = pathSegments.indexOf("Download")
        return if (downloadIndex != -1) {
            pathSegments.drop(downloadIndex + 1).dropLast(1).joinToString("/") + "/"
        } else ""
    }

    private fun isExternalStorageDocument(uri: Uri) =
        "com.android.externalstorage.documents" == uri.authority

    private fun isDownloadsDocument(uri: Uri) =
        "com.android.providers.downloads.documents" == uri.authority

    private fun isRawDownloadsDocument(uri: Uri) = uri.toString().contains("raw")
    private fun isMediaDocument(uri: Uri) = "com.android.providers.media.documents" == uri.authority
    private fun isGooglePhotosUri(uri: Uri) =
        "com.google.android.apps.photos.content" == uri.authority
}