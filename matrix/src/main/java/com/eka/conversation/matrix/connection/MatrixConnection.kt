package com.eka.conversation.matrix.connection

import kotlinx.serialization.json.JsonObject

enum class DisconnectReason {
    CLIENT_CLOSED, SERVER_CLOSED, CONNECTION_ERROR, AUTHENTICATION_ERROR, TIMEOUT, NETWORK_ERROR
}

data class DisconnectionDetails(
    val reason: DisconnectReason,
    val message: String? = null,
    val code: Int? = null,
)

/** Callbacks are always delivered on the main thread. */
interface ConnectionListener {
    fun onOpen()
    fun onMessage(message: JsonObject)
    fun onError(error: Throwable)
    fun onDisconnect(details: DisconnectionDetails)
}

/** Port of synapse `BaseConnection`: one transport per session. */
abstract class MatrixConnection(val sessionId: String) {
    @Volatile protected var listener: ConnectionListener? = null
    @Volatile private var disconnected: DisconnectionDetails? = null

    fun bind(l: ConnectionListener) {
        listener = l
    }

    abstract fun connect()
    abstract fun send(message: JsonObject)
    abstract fun close()
    abstract fun isConnected(): Boolean

    /** Called once the server confirmed the session (WS `conn` event / stream open). */
    open fun handleConnected() {}

    /** Fires onDisconnect at most once per connection. */
    protected fun disconnect(details: DisconnectionDetails) {
        if (disconnected != null) return
        disconnected = details
        listener?.onDisconnect(details)
    }

    companion object {
        /** Close codes that carry a server verdict. */
        const val CLOSE_AUTH_EXPIRED = 4401
        const val CLOSE_FORBIDDEN = 4403
        const val CLOSE_SESSION_GONE = 4404
        const val CLOSE_TRY_LATER = 1013
    }
}
