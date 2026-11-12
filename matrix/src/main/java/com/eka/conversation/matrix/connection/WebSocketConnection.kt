package com.eka.conversation.matrix.connection

import com.eka.conversation.matrix.MatrixLog
import com.eka.conversation.matrix.MatrixJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * Port of synapse `WebSocketConnection`.
 * URL: {wsBase}/ws/med-assist/session/{sessionId}/ ; auth is the first frame ({ev:"auth"}), sent by the client on open.
 */
class WebSocketConnection(
    sessionId: String,
    private val wsBaseUrl: String,
    private val okHttp: OkHttpClient,
    private val scope: CoroutineScope,
) : MatrixConnection(sessionId) {

    @Volatile private var socket: WebSocket? = null
    @Volatile private var open = false
    @Volatile private var closedByClient = false

    override fun connect() {
        val url = "${wsBaseUrl.trimEnd('/')}/ws/med-assist/session/$sessionId/"
        MatrixLog.net("WS connect -> $url")
        socket = okHttp.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                MatrixLog.net("WS open <- ${response.code}")
                open = true
                post { listener?.onOpen() }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                MatrixLog.net("WS <- ${MatrixLog.redact(text)}")
                val json = try {
                    MatrixJson.parseToJsonElement(text) as? JsonObject
                } catch (e: Exception) {
                    post { listener?.onError(e) }
                    null
                } ?: return
                post { listener?.onMessage(json) }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                MatrixLog.net("WS closing <- code=$code reason=$reason")
                webSocket.close(code, reason)
                handleClose(code, reason, clean = true)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                handleClose(code, reason, clean = true)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                MatrixLog.net("WS failure: ${t.message} (http=${response?.code})")
                handleClose(1006, t.message ?: "Network connection lost", clean = false)
            }
        })
    }

    private fun handleClose(code: Int, reason: String, clean: Boolean) {
        open = false
        if (closedByClient) return
        val details = when {
            code == 1000 -> DisconnectionDetails(DisconnectReason.CLIENT_CLOSED, "Connection closed normally", code)
            code == 1001 || code == 1006 -> DisconnectionDetails(DisconnectReason.NETWORK_ERROR, reason.ifBlank { "Network connection lost" }, code)
            code == 4001 || code == 1008 -> if (reason == "timeout" || reason == "Authentication timeout")
                DisconnectionDetails(DisconnectReason.TIMEOUT, "Connection timed out", code)
            else DisconnectionDetails(DisconnectReason.AUTHENTICATION_ERROR, reason.ifBlank { "Authentication failed" }, code)
            code == CLOSE_AUTH_EXPIRED || code == CLOSE_FORBIDDEN -> DisconnectionDetails(
                DisconnectReason.AUTHENTICATION_ERROR,
                reason.ifBlank { if (code == CLOSE_AUTH_EXPIRED) "Session token expired" else "Session credentials rejected" },
                code
            )
            code == CLOSE_SESSION_GONE -> DisconnectionDetails(DisconnectReason.SERVER_CLOSED, reason.ifBlank { "Session no longer exists" }, code)
            code == CLOSE_TRY_LATER -> DisconnectionDetails(DisconnectReason.SERVER_CLOSED, reason.ifBlank { "Server busy, try again later" }, code)
            clean -> DisconnectionDetails(DisconnectReason.SERVER_CLOSED, reason.ifBlank { "Server closed connection" }, code)
            else -> DisconnectionDetails(DisconnectReason.CONNECTION_ERROR, reason.ifBlank { "Connection closed unexpectedly" }, code)
        }
        post { disconnect(details) }
    }

    override fun send(message: JsonObject) {
        val ws = socket
        check(ws != null && open) { "Cannot send message: connection not open" }
        MatrixLog.net("WS -> ${MatrixLog.redact(message.toString())}")
        if (!ws.send(message.toString())) throw IllegalStateException("Failed to send WebSocket message")
    }

    override fun close() {
        closedByClient = true
        open = false
        socket?.close(1000, "Client closed connection")
        socket = null
    }

    override fun isConnected(): Boolean = open

    private fun post(block: () -> Unit) {
        scope.launch(Dispatchers.Main) { block() }
    }
}
