package com.eka.conversation.data.remote.socket.events.receive

import androidx.annotation.Keep
import com.eka.conversation.data.remote.socket.events.BaseSocketEvent
import com.eka.conversation.data.remote.socket.events.SocketEventType
import com.google.gson.annotations.SerializedName

@Keep
data class ConnectionEvent(
    @SerializedName("ts")
    override val timeStamp: Long? = null,
    @SerializedName("ev")
    override val eventType: SocketEventType,
    @SerializedName("data")
    val data: ConnectionData? = null,
    @SerializedName("sid")
    val sessionId: String? = null,
    @SerializedName("msg")
    val message: String? = null
) : BaseSocketEvent {
    fun resolvedSessionId(): String? = sessionId ?: data?.sessionId
}

@Keep
data class ConnectionData(
    @SerializedName("sid")
    val sessionId: String
)