package com.eka.conversation.common

import android.util.Log
import com.eka.conversation.BuildConfig

object ChatLogger {
    var enableDebugLogs = BuildConfig.IS_DEBUG
    fun d(tag: String, msg: String) {
        if (enableDebugLogs) {
            Log.d(tag, msg)
        }
    }

    fun e(tag: String, msg: String, e: Exception? = null) {
        if (enableDebugLogs) {
            Log.e(tag, msg, e)
        }
    }

    fun w(tag: String, msg: String) {
        if (enableDebugLogs) {
            Log.w(tag, msg)
        }
    }

    fun i(tag: String, msg: String) {
        if (enableDebugLogs) {
            Log.i(tag, msg)
        }
    }
}