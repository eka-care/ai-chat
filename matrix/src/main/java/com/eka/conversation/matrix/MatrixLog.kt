package com.eka.conversation.matrix

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log

/**
 * Logcat under tag "MedAssist", only in debuggable apps (debug / releaseDebug).
 * Logs metadata only (levels, ids, event types, codes), never message text or tokens.
 */
internal object MatrixLog {
    private const val TAG = "MedAssist"
    @Volatile private var enabled = false

    fun init(context: Context) {
        enabled = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    fun d(msg: String) { if (enabled) Log.d(TAG, msg) }
    fun w(msg: String) { if (enabled) Log.w(TAG, msg) }

    /** First 8 chars of an id, enough to correlate without dumping it. */
    fun id(value: String?) = value?.take(8) ?: "none"
}
