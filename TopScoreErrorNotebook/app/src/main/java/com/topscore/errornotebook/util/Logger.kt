package com.topscore.errornotebook.util

import android.util.Log

/**
 * Logger utility for debugging
 * Only logs in debug builds
 */
object Logger {
    private const val TAG = "TopScore"

    // Debug mode flag - set to false in release builds if needed
    private const val isDebug = true

    fun d(tag: String, message: String) {
        if (isDebug) {
            Log.d("$TAG.$tag", message)
        }
    }

    fun i(tag: String, message: String) {
        if (isDebug) {
            Log.i("$TAG.$tag", message)
        }
    }

    fun w(tag: String, message: String) {
        if (isDebug) {
            Log.w("$TAG.$tag", message)
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (isDebug) {
            if (throwable != null) {
                Log.e("$TAG.$tag", message, throwable)
            } else {
                Log.e("$TAG.$tag", message)
            }
        }
    }

    // Convenience methods for common modules
    object Auth {
        fun d(message: String) = Logger.d("Auth", message)
        fun i(message: String) = Logger.i("Auth", message)
        fun w(message: String) = Logger.w("Auth", message)
        fun e(message: String, t: Throwable? = null) = Logger.e("Auth", message, t)
    }

    object OCR {
        fun d(message: String) = Logger.d("OCR", message)
        fun i(message: String) = Logger.i("OCR", message)
        fun w(message: String) = Logger.w("OCR", message)
        fun e(message: String, t: Throwable? = null) = Logger.e("OCR", message, t)
    }

    object Question {
        fun d(message: String) = Logger.d("Question", message)
        fun i(message: String) = Logger.i("Question", message)
        fun w(message: String) = Logger.w("Question", message)
        fun e(message: String, t: Throwable? = null) = Logger.e("Question", message, t)
    }

    object Sync {
        fun d(message: String) = Logger.d("Sync", message)
        fun i(message: String) = Logger.i("Sync", message)
        fun w(message: String) = Logger.w("Sync", message)
        fun e(message: String, t: Throwable? = null) = Logger.e("Sync", message, t)
    }
}
