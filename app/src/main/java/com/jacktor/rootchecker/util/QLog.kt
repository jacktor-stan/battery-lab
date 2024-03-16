@file:Suppress("unused")

package com.jacktor.rootchecker.util

import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter

object QLog {
    const val NONE = 0
    private const val ERRORS_ONLY = 1
    private const val ERRORS_WARNINGS = 2
    private const val ERRORS_WARNINGS_INFO = 3
    private const val ERRORS_WARNINGS_INFO_DEBUG = 4
    const val ALL = 5
    var LOGGING_LEVEL = ALL

    /*
     * For filtering app specific output
     */
    private const val TAG = "RootChecker"

    /*
     * So any important logs can be outputted in non filtered output also
     */
    private const val TAG_GENERAL_OUTPUT = "QLog"

    /**
     * @param obj the object to log
     * @param cause
     * The exception which caused this error, may not be null
     */
    fun e(obj: Any, cause: Throwable) {
        if (isELoggable) {
            Log.e(TAG, trace + obj.toString())
            Log.e(TAG, getThrowableTrace(cause))
            Log.e(TAG_GENERAL_OUTPUT, trace + obj.toString())
            Log.e(TAG_GENERAL_OUTPUT, getThrowableTrace(cause))
        }
    }

    fun e(obj: Any) {
        if (isELoggable) {
            Log.e(TAG, trace + obj.toString())
            Log.e(TAG_GENERAL_OUTPUT, trace + obj.toString())
        }
    }

    fun e(e: Exception) {
        if (isELoggable) {
            e.printStackTrace()
        }
    }

    fun w(obj: Any, cause: Throwable) {
        if (isWLoggable) {
            Log.w(TAG, trace + obj.toString())
            Log.w(TAG, getThrowableTrace(cause))
            Log.w(TAG_GENERAL_OUTPUT, trace + obj.toString())
            Log.w(TAG_GENERAL_OUTPUT, getThrowableTrace(cause))
        }
    }

    fun w(obj: Any) {
        if (isWLoggable) {
            Log.w(TAG, trace + obj.toString())
            Log.w(TAG_GENERAL_OUTPUT, trace + obj.toString())
        }
    }

    fun i(obj: Any) {
        if (isILoggable) {
            Log.i(TAG, trace + obj.toString())
        }
    }

    fun d(obj: Any) {
        if (isDLoggable) {
            Log.d(TAG, trace + obj.toString())
        }
    }

    fun v(obj: Any) {
        if (isVLoggable) {
            Log.v(TAG, trace + obj.toString())
        }
    }

    private val isVLoggable: Boolean
        get() = LOGGING_LEVEL > ERRORS_WARNINGS_INFO_DEBUG
    private val isDLoggable: Boolean
        get() = LOGGING_LEVEL > ERRORS_WARNINGS_INFO
    private val isILoggable: Boolean
        get() = LOGGING_LEVEL > ERRORS_WARNINGS
    private val isWLoggable: Boolean
        get() = LOGGING_LEVEL > ERRORS_ONLY
    private val isELoggable: Boolean
        get() = LOGGING_LEVEL > NONE

    private fun getThrowableTrace(thr: Throwable): String {
        val b = StringWriter()
        thr.printStackTrace(PrintWriter(b))
        return b.toString()
    }

    private val trace: String
        get() {
            val depth = 2
            val t = Throwable()
            val elements = t.stackTrace
            val callerMethodName = elements[depth].methodName
            val callerClassPath = elements[depth].className
            val lineNo = elements[depth].lineNumber
            val i = callerClassPath.lastIndexOf('.')
            val callerClassName = callerClassPath.substring(i + 1)
            return (callerClassName + ": " + callerMethodName + "() ["
                    + lineNo + "] - ")
        }
}