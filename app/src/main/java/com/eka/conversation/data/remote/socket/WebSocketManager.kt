package com.eka.conversation.data.remote.socket

import com.eka.conversation.common.ChatLogger
import com.eka.conversation.data.remote.socket.states.SocketConnectionState
import com.eka.conversation.data.remote.socket.states.SocketMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.util.concurrent.TimeUnit

class WebSocketManager(
    private val url: String,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    private val maxReconnectAttempts: Int = 3,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private val _connectionState = MutableStateFlow<SocketConnectionState>(
        SocketConnectionState.Idle
    )

    private val _events = MutableSharedFlow<SocketMessage>()

    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0
    private val baseReconnectDelay = 1000L

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            ChatLogger.d(TAG, response.isSuccessful.toString())
            _connectionState.value = SocketConnectionState.Connected
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            ChatLogger.d(TAG, "onMessage TextType $text")
            _events.tryEmit(SocketMessage.TextMessage(text = text))
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            ChatLogger.d(TAG, "onMessage BytesType $bytes")
            _events.tryEmit(SocketMessage.ByteStringMessage(bytes = bytes))
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            ChatLogger.d(TAG, "OnClosing Code : $code Reason : $reason")
            _connectionState.value = SocketConnectionState.Disconnecting
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            ChatLogger.d(TAG, "onClosed Code : $code Reason : $reason")
            _connectionState.value = SocketConnectionState.Disconnected
            if (code != 1000) {
                scheduleReconnect()
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            ChatLogger.d(TAG, "onFailure : Reason : ${t.localizedMessage}")
            t.printStackTrace()
            _connectionState.value = SocketConnectionState.Error(Exception(t))
        }
    }

    fun listenEvents() = _events.asSharedFlow()

    fun listenConnectionState() = _connectionState.asStateFlow()

    // reconnect when it is closed because of non user action in reason code in onClosed.
    fun sendText(message: String): Boolean {
        return webSocket?.send(message) ?: false
    }

    fun sendBytes(bytes: ByteArray): Boolean {
        return webSocket?.send(bytes.toByteString()) ?: false
    }

    fun connect() {
        if (_connectionState.value is SocketConnectionState.Connected || _connectionState.value is SocketConnectionState.Connecting) {
            ChatLogger.w(TAG, "Already connected or connecting")
            return
        }

        _connectionState.value = SocketConnectionState.Connecting

        val request = Request.Builder()
            .url(url)
            .build()

        try {
            webSocket = client.newWebSocket(request, listener)
        } catch (e: Exception) {
            _connectionState.value = SocketConnectionState.Error(e)
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        if (reconnectAttempts >= maxReconnectAttempts) {
            return
        }

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val delay = baseReconnectDelay * (1 shl reconnectAttempts)
            ChatLogger.d(TAG, "Reconnecting in ${delay}ms (attempt ${reconnectAttempts + 1})")

            delay(delay)
            reconnectAttempts++
            connect()
        }
    }

    fun disconnect() {
        reconnectJob?.cancel()
        webSocket?.close(1000, "User disconnect")
        webSocket = null
    }

    fun cleanup() {
        disconnect()
        scope.cancel()
        client.dispatcher.executorService.shutdown()
    }

    companion object {
        private const val TAG = "WebSocketManager"

        @Volatile
        private var INSTANCE: WebSocketManager? = null

        fun getDatabase(
            url: String,
            scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            maxReconnectAttempts: Int = 3,
        ): WebSocketManager {
            return (INSTANCE ?: synchronized(this) {
                val instance = WebSocketManager(
                    url = url,
                    scope = scope,
                    maxReconnectAttempts = maxReconnectAttempts
                )
                INSTANCE = instance
                return instance
            })
        }
    }
}