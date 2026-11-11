package com.eka.conversation.matrix

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.eka.conversation.matrix.connection.ConnectionListener
import com.eka.conversation.matrix.connection.DisconnectReason
import com.eka.conversation.matrix.connection.DisconnectionDetails
import com.eka.conversation.matrix.connection.HttpStreamConnection
import com.eka.conversation.matrix.connection.MatrixConnection
import com.eka.conversation.matrix.connection.WebSocketConnection
import com.eka.conversation.matrix.media.FileUploadManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/** Matrix hosts per environment (same hosts the MedAssist web widget uses). */
enum class MatrixEnvironment(val baseUrl: String, val wsUrl: String) {
    PRODUCTION("https://matrix.eka.care/reloaded", "wss://matrix.eka.care/reloaded"),
    DEVELOPMENT("https://matrix.dev.eka.care/reloaded", "wss://matrix.dev.eka.care/reloaded"),
}

data class MatrixClientConfig(
    val agentId: String,
    /** Partner token sent as auth_token in the token exchange; null -> guest. */
    val authToken: String?,
    val environment: MatrixEnvironment = MatrixEnvironment.PRODUCTION,
    /** Override only for custom hosts; defaults follow [environment]. */
    val baseUrl: String = environment.baseUrl,
    val wsUrl: String = environment.wsUrl,
    val channel: String = "web",
    /** Session context: profile/intent/etc. (TContext). */
    val context: JsonObject? = null,
    /** Fallback only - the user_id from the token exchange wins. */
    val userId: String? = null,
)

/**
 * Kotlin port of synapse-sdk `SynapseSDK` (+ its MessageManager): token exchange, session lifecycle,
 * WebSocket / HTTP-stream transport, message envelopes, file-upload handshake, tool calls,
 * feedback, history, and the close-code driven recovery state machine.
 *
 * All public calls and all [events] run on the main thread.
 */
class MatrixClient(
    private val context: Context,
    private val config: MatrixClientConfig,
    seeded: StoredCredentials,
    private val onTokenExchange: (TokenExchangeResponse) -> Unit,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val http = MatrixHttpClient(config.baseUrl, config.agentId, config.channel)
    private val api = MatrixApi(http)
    private val tokens = TokenManager(api, http, config.agentId, config.authToken, seeded, onTokenExchange)
    private val files = FileUploadManager(http.okHttp)

    private val _events = MutableSharedFlow<MatrixEvent>(extraBufferCapacity = 256)
    val events: SharedFlow<MatrixEvent> = _events.asSharedFlow()
    private val _errors = MutableSharedFlow<MatrixError>(extraBufferCapacity = 32)
    val errors: SharedFlow<MatrixError> = _errors.asSharedFlow()

    private var connection: MatrixConnection? = null
    private var connectionType: String = SOCKET
    var sessionId: String? = null; private set
    private var outgoingBuffer: OutgoingRequest? = null
    private var agentConfig: AgentConfig? = null

    private var isRecovering = false
    private var isReconnecting = false
    private var isSessionNotOwned = false
    private var connectionAttempts = 0
    private var reconnectJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    init {
        MatrixLog.init(context)
        MatrixLog.d("client created: base=${config.baseUrl}, agent=${MatrixLog.id(config.agentId)}, partnerToken=${config.authToken != null}")
        // A 401 on ANY REST call: refresh the pair, rebuild a live socket with the new token, retry the call.
        http.setUnauthorizedHandler { onRestUnauthorized() }
    }

    val level: String? get() = tokens.level
    val userId: String? get() = tokens.userId

    // ------------------------------------------------------------------ public API

    suspend fun getAgentConfig(): AgentConfig? {
        agentConfig?.let { return it }
        return try {
            api.agentConfig(config.agentId).also { cfg ->
                agentConfig = cfg
                connectionType = if (cfg?.connectivity == HTTP_STREAM) HTTP_STREAM else SOCKET
                MatrixLog.d("agent config: name=${cfg?.name}, connectivity=${cfg?.connectivity}, allowed=${cfg?.allowed}")
            }
        } catch (e: Throwable) {
            MatrixLog.w("agent config failed: ${e.message}")
            null
        }
    }

    /**
     * Start (or resume) a session. No pre-flight: the transport handshake validates a resumed
     * session/pair and [handleRecovery] repairs whatever the server rejects.
     */
    suspend fun startSession(existingSessionId: String? = null): SessionResponse {
        try {
            tokens.ensureCredentials()
            val session = if (!existingSessionId.isNullOrBlank()) SessionResponse(sessionId = existingSessionId)
            else createNewSession()
            MatrixLog.d("session ${if (existingSessionId.isNullOrBlank()) "created" else "resumed"}: ${MatrixLog.id(session.sessionId)}")
            initializeConnection(session.sessionId)
            return session
        } catch (e: Throwable) {
            MatrixLog.w("startSession failed: ${e.message} (status=${(e as? MatrixApiException)?.status})")
            emitError(e)
            throw e
        }
    }

    fun isConnected(): Boolean = connection?.isConnected() == true && connectionEstablished

    fun sendMessage(request: OutgoingRequest) {
        val conn = connection
        if (conn == null || !isConnected()) {
            MatrixLog.d("not connected yet: message buffered")
            outgoingBuffer = request
            return
        }
        try {
            val envelope = buildOutgoing(request) ?: run {
                emitError(MatrixError("No message to send", MatrixErrorCode.MESSAGE))
                return
            }
            MatrixLog.d("-> ${envelope.str("ev")}/${envelope.str("ct")}" + (if (request.toolId != null) " tool=${MatrixLog.id(request.toolId)}" else ""))
            conn.send(envelope)
        } catch (e: Throwable) {
            emitError(MatrixError(e.message ?: "Failed to send message", MatrixErrorCode.CONNECTION))
        }
    }

    /** POST tool call; the reply is re-emitted as a TOOL_CALL event, like the live socket. */
    suspend fun callTool(toolId: String, messageId: String, params: JsonObject?) {
        val sid = sessionId ?: throw IllegalStateException("Session ID not found")
        check(connection != null) { "Connection not established" }
        MatrixLog.d("callTool tool=${MatrixLog.id(toolId)} msg=${MatrixLog.id(messageId)} keys=${params?.keys}")
        val response = api.callTool(sid, toolId, messageId, params)
        (response["error"] as? JsonObject)?.let { err ->
            throw MatrixApiException(500, err.str("msg") ?: "Tool call failed", ApiErrorBody(err.str("code"), err.str("msg")))
        }
        val data = response["data"] as? JsonObject ?: JsonObject(emptyMap())
        val id = response.str("_id") ?: messageId
        emitToolCall(id, (response["ts"] as? JsonPrimitive)?.longOrNull, data)
    }

    suspend fun sendMessageFeedback(messageId: String, feedback: String, reason: String? = null) {
        val sid = sessionId ?: return
        api.feedback(sid, messageId = messageId, feedback = feedback, reason = reason)
    }

    suspend fun sendSessionFeedback(feedbacks: List<String>?, npsBase5: Int?) {
        val sid = sessionId ?: return
        require(!feedbacks.isNullOrEmpty() || npsBase5 != null) { "No feedback provided" }
        api.feedback(sid, feedbacks = feedbacks, npsBase5 = npsBase5)
    }

    suspend fun pastSessions(): List<PastSession> = api.pastSessions().data

    suspend fun sessionMessages(id: String): List<StoredMessage> =
        api.retrieveSession(id, includeMessages = true).messages.orEmpty()

    fun clearTokens() = tokens.clearTokens()

    /** Called when the screen returns to foreground (mirrors the web visibility reconnect). */
    fun onForeground() {
        if (isConnected() || sessionId == null) return
        connectionAttempts = 0
        isReconnecting = false
        scheduleReconnect(DisconnectionDetails(DisconnectReason.NETWORK_ERROR, "Reconnecting after resume", 1006))
    }

    /** Tear down the transport and timers (keeps credentials). */
    fun endSession() {
        closeConnection()
        reconnectJob?.cancel()
        reconnectJob = null
        connectionAttempts = 0
        isReconnecting = false
        outgoingBuffer = null
        files.clear()
        unregisterNetworkCallback()
    }

    fun destroy() {
        endSession()
        scope.cancel()
    }

    // ------------------------------------------------------------------ connection

    private var connectionEstablished = false

    private suspend fun createNewSession(): SessionResponse {
        val ctx = config.context
        val profile = ctx?.get("profile") as? JsonObject
        val extra = ctx?.filterKeys { it != "profile" && it != "referer" }.orEmpty()
        val request = SessionCreateRequest(
            userId = tokens.userId ?: config.userId,
            userContext = profile,
            sessionContext = if (extra.isNotEmpty()) JsonObject(extra) else null,
            referer = ctx?.str("referer"),
        )
        val s = api.createSession(request)
        if (s.sessionId.isBlank()) throw MatrixApiException(500, "Failed to start session.", s.error)
        return s
    }

    private fun initializeConnection(id: String) {
        closeConnection()
        check(id.isNotBlank() && !tokens.sessToken.isNullOrBlank()) { "Session ID or sess token not found" }
        sessionId = id
        val conn: MatrixConnection = if (connectionType == HTTP_STREAM) {
            HttpStreamConnection(
                sessionId = id,
                baseUrl = config.baseUrl,
                okHttp = http.okHttp,
                scope = scope,
                headers = { http.snapshotHeaders() },
                getToken = { tokens.sessToken },
                onUnauthorized = { tokens.refreshBearer() },
            )
        } else {
            WebSocketConnection(id, config.wsUrl, http.okHttp, scope)
        }
        connectionAttempts++
        MatrixLog.d("connecting via $connectionType (attempt $connectionAttempts)")
        connectionEstablished = false
        connection = conn
        conn.bind(listenerFor(conn))
        conn.connect()
        registerNetworkCallback()
    }

    private fun listenerFor(conn: MatrixConnection) = object : ConnectionListener {
        override fun onOpen() {
            if (connection !== conn) return
            if (connectionType == SOCKET) {
                val token = tokens.sessToken
                if (token == null) {
                    emitError(MatrixError("Sess token not set", MatrixErrorCode.SESSION)); return
                }
                safeSend(conn, envelope(EV_AUTH) { put("data", buildJsonObject { put("token", token) }) })
            } else {
                handleConnectionEstablished(conn)
            }
        }

        override fun onMessage(message: JsonObject) {
            if (connection !== conn) return
            onIncoming(conn, message)
        }

        override fun onError(error: Throwable) {
            if (connection !== conn) return
            onRealtimeConnectionError(error)
        }

        override fun onDisconnect(details: DisconnectionDetails) {
            if (connection !== conn) return
            if (details.reason == DisconnectReason.CLIENT_CLOSED) return
            MatrixLog.w("disconnected: ${details.reason} code=${details.code} ${details.message}")
            connectionEstablished = false
            val outcome = outcomeForCloseCode(details.code)
            if (outcome != null) {
                scope.launch { handleRecovery(outcome, details) }
                return
            }
            if (isReconnecting) return
            scheduleReconnect(details)
        }
    }

    private fun handleConnectionEstablished(conn: MatrixConnection) {
        MatrixLog.d("connected")
        conn.handleConnected()
        connectionEstablished = true
        emit(MatrixEvent.Connected)
        outgoingBuffer?.let { buffered ->
            outgoingBuffer = null
            sendMessage(buffered)
        }
    }

    private fun closeConnection() {
        connection?.close()
        connection = null
        connectionEstablished = false
    }

    private fun safeSend(conn: MatrixConnection, msg: JsonObject) {
        try {
            conn.send(msg)
        } catch (e: Throwable) {
            emitError(MatrixError(e.message ?: "Send failed", MatrixErrorCode.CONNECTION))
        }
    }

    // ------------------------------------------------------------------ incoming

    private fun onIncoming(conn: MatrixConnection, msg: JsonObject) {
        val ev = msg.str("ev")
        val ct = msg.str("ct")
        val id = msg.str("_id")
        val ts = (msg["ts"] as? JsonPrimitive)?.longOrNull
        val data = msg["data"] as? JsonObject ?: JsonObject(emptyMap())
        if (ev != EV_STREAM || ct != CT_TEXT) MatrixLog.d("<- $ev${ct?.let { "/$it" } ?: ""} id=${MatrixLog.id(id)}")
        when (ev) {
            EV_CONN -> handleConnectionEstablished(conn)
            EV_CHAT -> when (ct) {
                CT_FILE -> {
                    val urls = (data["urls"] as? JsonArray)?.mapNotNull {
                        try { MatrixJson.decodeFromJsonElement<PresignedUrl>(it) } catch (_: Exception) { null }
                    }.orEmpty()
                    if (urls.isNotEmpty()) scope.launch {
                        try {
                            val ids = files.upload(urls)
                            sendMessage(OutgoingRequest(urls = ids))
                        } catch (e: Throwable) {
                            emitError(MatrixError(e.message ?: "Failed to upload files", MatrixErrorCode.FILE))
                        }
                    }
                }
                CT_TEXT -> {
                    val text = data.str("text")
                    if (text != null && text.isBlank() && id != null) emit(MatrixEvent.MessageChunk(id, text, ts))
                }
                CT_TOOL -> if (data.str("tool_id") != null && id != null) emitToolCall(id, ts, data)
                CT_AUDIO_TRANSCRIPT -> data.str("text")?.let { emit(MatrixEvent.AudioTranscript(it)) }
            }
            EV_STREAM -> when (ct) {
                CT_TEXT -> {
                    val progress = data.str("progress_msg")
                    if (!progress.isNullOrEmpty()) {
                        emit(MatrixEvent.Progress(id, progress)); return
                    }
                    val text = data.str("text")
                    if (!text.isNullOrEmpty() && id != null) emit(MatrixEvent.MessageChunk(id, text, ts))
                }
                CT_TIPS -> (data["tips"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                    ?.let { emit(MatrixEvent.Tips(it)) }
                CT_TOOL -> if (data.str("tool_id") != null && id != null) emitToolCall(id, ts, data)
                CT_TOOL_START -> emit(MatrixEvent.ToolStart(id ?: System.currentTimeMillis().toString(), data.str("name")))
                CT_TOOL_END -> emit(MatrixEvent.ToolEnd(id ?: System.currentTimeMillis().toString(), data.str("name")))
            }
            EV_EOS -> emit(MatrixEvent.EndOfStream)
            EV_PING -> safeSend(conn, envelope(EV_PONG))
            EV_PONG -> safeSend(conn, envelope(EV_PING))
            EV_SYNC -> Unit
            EV_ERROR -> handleIncomingError(data)
        }
    }

    private fun handleIncomingError(data: JsonObject) {
        val code = data.str("code")
        MatrixLog.w("server error frame: code=$code msg=${data.str("msg")}")
        when (code) {
            RealtimeErrorCodes.SESSION_TOKEN_EXPIRED -> scope.launch { handleAuthExpiry() }
            RealtimeErrorCodes.SESSION_INACTIVE -> scope.launch { handleSessionInactive() }
            RealtimeErrorCodes.SESSION_NOT_OWNED -> {
                // Claim the incoming 4404 so no reconnect path revives a session that isn't ours.
                isSessionNotOwned = true
                sessionId = null
                emit(MatrixEvent.RealtimeError(code, data.str("msg")))
            }
            else -> emit(MatrixEvent.RealtimeError(code, data.str("msg")))
        }
    }

    private fun emitToolCall(messageId: String, ts: Long?, data: JsonObject) {
        val parsed = try {
            MatrixJson.decodeFromJsonElement<ToolCallData>(data)
        } catch (_: Exception) {
            return
        }
        emit(MatrixEvent.ToolCall(messageId, ts, parsed, data))
    }

    // ------------------------------------------------------------------ outgoing

    private fun buildOutgoing(r: OutgoingRequest): JsonObject? {
        if (!r.files.isNullOrEmpty() || !r.urls.isNullOrEmpty()) {
            if ((r.files?.size ?: 0) > 3) throw IllegalArgumentException("Maximum 3 files are allowed to be sent at a time")
            return if (!r.files.isNullOrEmpty()) {
                files.setFilesForUpload(r.files, r.message)
                envelope(EV_CHAT, CT_FILE) {
                    put("data", buildJsonObject {
                        put("extensions", JsonArray(files.formats.map(::JsonPrimitive)))
                        put("file_sizes", JsonArray(files.sizes.map { JsonPrimitive(it) }))
                        if (r.toolDeclined) put("tool_declined", true)
                        r.toolResult?.let { put("tool_result", it) }
                    })
                }
            } else {
                val text = files.pendingMessage
                files.clear()
                envelope(EV_CHAT, CT_FILE) {
                    put("data", buildJsonObject {
                        put("urls", JsonArray(r.urls.orEmpty().map(::JsonPrimitive)))
                        if (text.isNotEmpty()) put("text", text)
                        if (r.toolDeclined) put("tool_declined", true)
                        r.toolResult?.let { put("tool_result", it) }
                    })
                }
            }
        }
        if (!r.message.isNullOrEmpty()) {
            return envelope(EV_CHAT, CT_TEXT) {
                put("data", buildJsonObject {
                    put("text", r.message)
                    if (r.toolDeclined) put("tool_declined", true)
                    r.toolId?.takeIf { it.isNotEmpty() }?.let { put("tool_id", it) }
                    r.initialPrompts?.let { prompts ->
                        put("initial_prompts", JsonArray(prompts.map { (role, text) ->
                            buildJsonObject { put("role", role); put("text", text) }
                        }))
                    }
                    r.toolResult?.takeIf { it.isNotEmpty() }?.let { put("tool_result", it) }
                })
            }
        }
        r.audio?.let { a ->
            return envelope(EV_CHAT, CT_AUDIO) {
                put("data", buildJsonObject {
                    put("audio", a.audio)
                    put("format", a.format)
                    if (r.toolDeclined) put("tool_declined", true)
                    r.toolResult?.let { put("tool_result", it) }
                })
            }
        }
        r.toolResult?.let { tr ->
            return envelope(EV_CHAT, CT_TEXT) {
                put("data", buildJsonObject {
                    put("text", "")
                    r.toolId?.takeIf { it.isNotEmpty() }?.let { put("tool_id", it) }
                    put("tool_result", tr)
                })
            }
        }
        return null
    }

    private inline fun envelope(ev: String, ct: String? = null, block: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit = {}): JsonObject {
        val now = System.currentTimeMillis()
        return buildJsonObject {
            put("ev", ev)
            ct?.let { put("ct", it) }
            put("_id", now.toString())
            put("ts", now)
            block()
        }
    }

    // ------------------------------------------------------------------ recovery

    private enum class Outcome { REFRESH_AND_RETRY, FORBIDDEN, SESSION_GONE, RETRY_LATER }

    private fun outcomeForCloseCode(code: Int?): Outcome? = when (code) {
        4401 -> Outcome.REFRESH_AND_RETRY
        4403 -> Outcome.FORBIDDEN
        4404 -> Outcome.SESSION_GONE
        1013 -> Outcome.RETRY_LATER
        else -> null
    }

    private fun outcomeForHttpStatus(status: Int): Outcome? = when (status) {
        401 -> Outcome.REFRESH_AND_RETRY
        403 -> Outcome.FORBIDDEN
        404 -> Outcome.SESSION_GONE
        429, 503 -> Outcome.RETRY_LATER
        else -> null
    }

    private fun onRealtimeConnectionError(error: Throwable) {
        if (error is MatrixApiException) {
            outcomeForHttpStatus(error.status)?.let { outcome ->
                scope.launch {
                    handleRecovery(outcome, DisconnectionDetails(DisconnectReason.CONNECTION_ERROR, error.message, error.status))
                }
                return
            }
        }
        emit(MatrixEvent.RealtimeError(null, error.message))
        emitError(if (error is MatrixApiException) MatrixError.from(error) else MatrixError(error.message ?: "Connection error", MatrixErrorCode.CONNECTION))
    }

    private suspend fun handleRecovery(outcome: Outcome, details: DisconnectionDetails?) {
        MatrixLog.w("recovery: $outcome (code=${details?.code})")
        if (outcome == Outcome.RETRY_LATER) {
            emitError(MatrixError("Server is busy, retrying shortly", MatrixErrorCode.SESSION, RecoveryReason.RETRY_LATER))
            details?.let { scheduleReconnect(it) }
            return
        }
        if (isRecovering) return
        isRecovering = true
        try {
            when (outcome) {
                Outcome.REFRESH_AND_RETRY -> recoverExpiredCredentials()
                Outcome.SESSION_GONE -> if (isSessionNotOwned) isSessionNotOwned = false else recoverMissingSession()
                Outcome.FORBIDDEN -> {
                    clearUser()
                    emitError(MatrixError("Session credentials were rejected", MatrixErrorCode.SESSION, RecoveryReason.SESSION_FORBIDDEN))
                }
                Outcome.RETRY_LATER -> Unit
            }
        } finally {
            isRecovering = false
        }
    }

    /** 4401/401: rotate the pair, reconnect the SAME session. */
    private suspend fun recoverExpiredCredentials() {
        try {
            closeConnection()
            tokens.refresh()
            sessionId?.let { initializeConnection(it) }
        } catch (e: Throwable) {
            val status = (e as? MatrixApiException)?.status
            if (status != 401 && status != 403) {
                emitError(MatrixError(e.message ?: "Failed to refresh authentication", MatrixErrorCode.SESSION))
                return
            }
            clearUser()
            emitError(MatrixError("Credentials were rejected", MatrixErrorCode.SESSION, RecoveryReason.SESSION_FORBIDDEN))
        }
    }

    /** 4404/404: guest -> ask for a new chat; user -> reinit via GET, else a fresh session. */
    private suspend fun recoverMissingSession() {
        val sid = sessionId
        val authenticated = tokens.level?.let { it == "user" } ?: (config.authToken != null)
        closeConnection()
        if (!authenticated || sid == null) {
            emitError(MatrixError("This chat is no longer available", MatrixErrorCode.SESSION, RecoveryReason.SESSION_NOT_FOUND))
            return
        }
        try {
            val s = api.retrieveSession(sid)
            initializeConnection(s.sessionId.ifBlank { sid })
        } catch (e: Throwable) {
            val status = (e as? MatrixApiException)?.status
            if (status == 401 || status == 403) {
                clearUser()
                emitError(MatrixError("Session credentials were rejected", MatrixErrorCode.SESSION, RecoveryReason.SESSION_FORBIDDEN))
                return
            }
            try {
                initializeConnection(createNewSession().sessionId)
            } catch (ce: Throwable) {
                val cs = (ce as? MatrixApiException)?.status
                if (cs == 401 || cs == 403) {
                    clearUser()
                    emitError(MatrixError("Session credentials were rejected", MatrixErrorCode.SESSION, RecoveryReason.SESSION_FORBIDDEN))
                } else {
                    emitError(MatrixError(ce.message ?: "Failed to start a new session", MatrixErrorCode.SESSION))
                }
            }
        }
    }

    private suspend fun handleAuthExpiry() {
        if (isRecovering) return
        isRecovering = true
        try {
            recoverExpiredCredentials()
        } finally {
            isRecovering = false
        }
    }

    private suspend fun handleSessionInactive() {
        if (isRecovering) return
        isRecovering = true
        try {
            recoverMissingSession()
        } finally {
            isRecovering = false
        }
    }

    private suspend fun onRestUnauthorized(): String? {
        val bearer = tokens.refreshBearer() ?: return null
        val sid = sessionId
        if (connectionType == SOCKET && sid != null && isConnected()) {
            try {
                initializeConnection(sid)
            } catch (e: Throwable) {
                emitError(MatrixError(e.message ?: "Failed to reset connection after auth refresh", MatrixErrorCode.CONNECTION))
            }
        }
        return bearer
    }

    private fun clearUser() {
        closeConnection()
        tokens.clearAll()
        sessionId = null
    }

    /** Exponential backoff: 1s, 2s, 4s, 8s, 16s; at most 5 attempts per connection budget. */
    private fun scheduleReconnect(details: DisconnectionDetails) {
        val sid = sessionId
        if (connectionAttempts >= 5 || sid == null) {
            emit(MatrixEvent.Disconnected(details))
            return
        }
        val delayMs = minOf(1000L * (1L shl connectionAttempts.coerceAtMost(4)), 16000L)
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(delayMs)
            val current = sessionId ?: return@launch
            if (!isOnline()) return@launch // the network callback reconnects
            isReconnecting = true
            try {
                startSession(current)
            } catch (_: Throwable) {
                if (connectionAttempts >= 5) emit(MatrixEvent.Disconnected(details))
            } finally {
                isReconnecting = false
            }
        }
    }

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private var wentOffline = false

    private fun registerNetworkCallback() {
        if (networkCallback != null) return
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                scope.launch { if (!isOnline()) handleNetworkLost() }
            }

            override fun onAvailable(network: Network) {
                scope.launch { handleNetworkAvailable() }
            }
        }
        try {
            // Default network only: fires onLost on airplane mode / no signal, onAvailable when it returns.
            cm.registerDefaultNetworkCallback(cb)
            networkCallback = cb
        } catch (_: Throwable) {
        }
    }

    private fun handleNetworkLost() {
        if (wentOffline) return
        MatrixLog.w("network lost: dropping socket")
        wentOffline = true
        reconnectJob?.cancel()
        // OkHttp keeps a dead socket "open" until the next ping fails; drop it now so
        // isConnected() is false and new sends are buffered instead of silently lost.
        closeConnection()
        emit(MatrixEvent.Offline)
    }

    private suspend fun handleNetworkAvailable() {
        // onAvailable also fires right after registration; only act after a real loss.
        if (!wentOffline) return
        wentOffline = false
        MatrixLog.d("network back: reconnecting")
        emit(MatrixEvent.Online)
        val sid = sessionId ?: return
        reconnectJob?.cancel()
        connectionAttempts = 0
        isReconnecting = false
        try {
            startSession(sid)
        } catch (_: Throwable) {
        }
    }

    private fun unregisterNetworkCallback() {
        val cb = networkCallback ?: return
        networkCallback = null
        wentOffline = false
        try {
            (context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)?.unregisterNetworkCallback(cb)
        } catch (_: Throwable) {
        }
    }

    // ------------------------------------------------------------------ emit

    private fun emit(event: MatrixEvent) {
        _events.tryEmit(event)
    }

    private fun emitError(error: MatrixError) {
        _errors.tryEmit(error)
    }

    private fun emitError(t: Throwable) {
        emitError(MatrixError.from(t, MatrixErrorCode.SESSION))
    }

    companion object {
        const val SOCKET = "socket"
        const val HTTP_STREAM = "http_stream"

        private const val EV_PING = "ping"
        private const val EV_PONG = "pong"
        private const val EV_CHAT = "chat"
        private const val EV_STREAM = "stream"
        private const val EV_AUTH = "auth"
        private const val EV_EOS = "eos"
        private const val EV_SYNC = "sync"
        private const val EV_ERROR = "err"
        private const val EV_CONN = "conn"

        private const val CT_TEXT = "text"
        private const val CT_AUDIO = "audio"
        private const val CT_FILE = "file"
        private const val CT_TOOL = "tool"
        private const val CT_TIPS = "tips"
        private const val CT_AUDIO_TRANSCRIPT = "audio_transcript"
        private const val CT_TOOL_START = "tool_start"
        private const val CT_TOOL_END = "tool_end"
    }
}
