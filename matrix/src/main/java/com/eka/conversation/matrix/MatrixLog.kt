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

    val isEnabled get() = enabled

    /**
     * Debug-only description of a token: never the token itself. Shortened (first 6 + last 4) and,
     * for a JWT, who issued it, for which app, and how long it has left.
     */
    fun token(value: String?): String {
        if (value.isNullOrBlank()) return "none"
        val short = if (value.length > 14) value.take(6) + "…" + value.takeLast(4) else "…"
        val parts = value.split('.')
        if (parts.size != 3) return "$short (opaque, len=${value.length})"
        return try {
            val raw = android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)
            val claims = MatrixJson.parseToJsonElement(String(raw, Charsets.UTF_8)) as kotlinx.serialization.json.JsonObject
            fun c(k: String) = claims.str(k)
            val exp = c("exp")?.toLongOrNull()
            val left = exp?.let { (it - System.currentTimeMillis() / 1000) / 60 }
            val life = if (left == null) "no exp" else if (left < 0) "EXPIRED ${-left}m ago" else "valid ${left}m"
            "$short (jwt iss=${c("iss")}, aud=${c("aud")}, $life)"
        } catch (_: Exception) {
            "$short (jwt, unreadable)"
        }
    }

    private const val NET_TAG = "MedAssistNet"
    private val SECRET_KEYS = setOf("auth_token", "refresh_token", "sess_token", "token", "authorization")

    /**
     * Full network log (debug builds only) under tag "MedAssistNet": every request, response and
     * socket frame. Tokens are shortened via [token], signed URLs lose their query, long blobs
     * (base64 audio/files) show only their length. Long lines are split for logcat.
     */
    fun net(msg: String) {
        if (!enabled) return
        msg.chunked(3500).forEachIndexed { i, part -> Log.d(NET_TAG, if (i == 0) part else "   …$part") }
    }

    /** A JSON (or plain) payload made safe to log. */
    fun redact(text: String?): String {
        if (text.isNullOrEmpty()) return "(empty)"
        return try {
            redactElement(null, MatrixJson.parseToJsonElement(text)).toString()
        } catch (_: Exception) {
            redactString(null, text)
        }
    }

    fun redactHeaders(headers: Map<String, String>): String =
        headers.entries.joinToString(", ", "{", "}") { (k, v) -> "$k: ${redactString(k, v)}" }

    fun redactUrl(url: String): String {
        val q = url.indexOf('?')
        if (q < 0) return url
        val query = url.substring(q + 1)
        val signed = listOf("sig=", "signature", "x-amz-", "se=", "sv=").any { query.contains(it, ignoreCase = true) }
        return if (signed) url.substring(0, q) + "?<signed query hidden>" else url
    }

    private fun redactElement(key: String?, e: kotlinx.serialization.json.JsonElement): kotlinx.serialization.json.JsonElement = when (e) {
        is kotlinx.serialization.json.JsonObject -> kotlinx.serialization.json.JsonObject(e.mapValues { (k, v) -> redactElement(k, v) })
        is kotlinx.serialization.json.JsonArray -> kotlinx.serialization.json.JsonArray(e.map { redactElement(key, it) })
        is kotlinx.serialization.json.JsonPrimitive ->
            if (e.isString) kotlinx.serialization.json.JsonPrimitive(redactString(key, e.content)) else e
        else -> e
    }

    private fun redactString(key: String?, value: String): String {
        val k = key?.lowercase()
        return when {
            k != null && k in SECRET_KEYS -> if (k == "authorization") "Bearer " + token(value.removePrefix("Bearer ")) else token(value)
            value.startsWith("http", ignoreCase = true) -> redactUrl(value)
            value.length > 600 -> "<${value.length} chars>"
            else -> value
        }
    }

    /** First 8 chars of an id, enough to correlate without dumping it. */
    fun id(value: String?) = value?.take(8) ?: "none"
}
