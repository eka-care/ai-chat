package com.eka.conversation.matrix.connection

import com.eka.conversation.matrix.MatrixLog
import com.eka.conversation.matrix.MatrixApiException
import com.eka.conversation.matrix.MatrixJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Port of synapse `SSEConnection` (connectivity = "http_stream").
 * Every outgoing envelope is POSTed to {base}/med-assist/session/{id}/chat and the
 * reply body streams back-to-back JSON objects ({..}{..}) with no separator.
 */
class HttpStreamConnection(
    sessionId: String,
    private val baseUrl: String,
    okHttp: OkHttpClient,
    private val scope: CoroutineScope,
    private val headers: () -> Map<String, String>,
    private val getToken: () -> String?,
    private val onUnauthorized: suspend () -> String?,
) : MatrixConnection(sessionId) {

    // Streams can idle while the model thinks; never time a read out.
    private val client = okHttp.newBuilder().readTimeout(0, TimeUnit.MILLISECONDS).build()

    @Volatile private var connected = false
    @Volatile private var activeJob: Job? = null
    @Volatile private var activeCall: Call? = null

    override fun connect() {
        connected = true
        // Mirrors the deferred onOpen of the JS transport: fire after the caller wired listeners.
        scope.launch(Dispatchers.Main) { listener?.onOpen() }
    }

    override fun send(message: JsonObject) {
        if (!connected) {
            listener?.onError(IllegalStateException("Stream connection is not open"))
            return
        }
        activeCall?.cancel()
        activeJob?.cancel()
        activeJob = scope.launch(Dispatchers.IO) { streamChat(message) }
    }

    private suspend fun streamChat(message: JsonObject) {
        val url = "${baseUrl.trimEnd('/')}/med-assist/session/$sessionId/chat"
        MatrixLog.net("STREAM -> POST $url\n  body: ${MatrixLog.redact(message.toString())}")
        try {
            var call = newCall(url, message)
            var response = call.execute()
            if (response.code == 401) {
                response.close()
                val bearer = onUnauthorized()
                if (bearer == null) throw MatrixApiException(401, "Unauthorized")
                call = newCall(url, message)
                response = call.execute()
            }
            response.use { res ->
                if (!res.isSuccessful) throw MatrixApiException(res.code, "Failed to send message to http stream")
                val source = res.body?.source() ?: throw MatrixApiException(res.code, "Empty stream body")
                MatrixLog.net("STREAM <- ${res.code}")
                val parser = JsonObjectStreamParser { obj ->
                    MatrixLog.net("STREAM <- ${MatrixLog.redact(obj.toString())}")
                    scope.launch(Dispatchers.Main) { listener?.onMessage(obj) }
                }
                val buffer = ByteArray(8 * 1024)
                val decoder = Charsets.UTF_8.newDecoder()
                val byteBuf = java.nio.ByteBuffer.allocate(16 * 1024)
                val charBuf = java.nio.CharBuffer.allocate(16 * 1024)
                val stream = source.inputStream()
                while (kotlinx.coroutines.currentCoroutineContext().isActive) {
                    val n = stream.read(buffer)
                    if (n == -1) break
                    var offset = 0
                    while (offset < n) {
                        val take = minOf(byteBuf.remaining(), n - offset)
                        byteBuf.put(buffer, offset, take)
                        offset += take
                        byteBuf.flip()
                        decoder.decode(byteBuf, charBuf, false)
                        byteBuf.compact()
                        charBuf.flip()
                        parser.feed(charBuf.toString())
                        charBuf.clear()
                    }
                }
                parser.finish()
            }
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException) return
            if (e is IOException && activeCall?.isCanceled() == true) return
            withContext(Dispatchers.Main) { listener?.onError(e) }
        }
    }

    private fun newCall(url: String, message: JsonObject): Call {
        val request = Request.Builder()
            .url(url)
            .apply {
                headers().forEach { (k, v) -> if (!k.equals("Authorization", true)) header(k, v) }
                getToken()?.let { header("Authorization", "Bearer $it") }
                header("Accept", "text/event-stream")
                header("Content-Type", "application/json")
            }
            .post(message.toString().toRequestBody("application/json".toMediaType()))
            .build()
        return client.newCall(request).also { activeCall = it }
    }

    override fun close() {
        connected = false
        activeCall?.cancel()
        activeJob?.cancel()
        activeCall = null
        activeJob = null
    }

    override fun isConnected(): Boolean = connected
}

/**
 * Splits a stream of concatenated JSON objects (optionally SSE-prefixed) into objects by
 * tracking brace depth while respecting strings/escapes.
 */
class JsonObjectStreamParser(private val onObject: (JsonObject) -> Unit) {
    private val buf = StringBuilder()

    fun feed(chunk: String) {
        buf.append(chunk)
        drain()
    }

    fun finish() = drain()

    private fun drain() {
        var i = 0
        while (true) {
            val start = buf.indexOf("{", i)
            if (start == -1) {
                buf.setLength(0); return
            }
            val end = findObjectEnd(start)
            if (end == -1) {
                buf.delete(0, start); return
            }
            val text = buf.substring(start, end + 1)
            try {
                (MatrixJson.parseToJsonElement(text) as? JsonObject)?.let(onObject)
            } catch (_: Exception) {
            }
            i = end + 1
        }
    }

    private fun findObjectEnd(start: Int): Int {
        var depth = 0
        var inString = false
        var escape = false
        for (i in start until buf.length) {
            val ch = buf[i]
            if (inString) {
                when {
                    escape -> escape = false
                    ch == '\\' -> escape = true
                    ch == '"' -> inString = false
                }
                continue
            }
            when (ch) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return -1
    }
}
