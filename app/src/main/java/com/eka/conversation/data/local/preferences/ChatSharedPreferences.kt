package com.eka.conversation.data.local.preferences

import android.content.Context
import androidx.core.content.edit

class ChatSharedPreferences(
    context: Context
) {
    companion object {
        const val SESSION_ID = "session_id"
        const val SESSION_TOKEN = "session_token"
    }

    private val pref = context.getSharedPreferences("medassist_pref", Context.MODE_PRIVATE)

    fun setString(key: String, value: String) {
        pref.edit { putString(key, value) }
    }

    fun getString(key: String): String? {
        return pref.getString(key, null)
    }
}