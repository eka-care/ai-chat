package com.eka.conversation.data.remote.socket.events

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class ConnectionEvent(
    @SerializedName("ts")
    override val timeStamp: Long,
    @SerializedName("ev")
    override val eventType: SocketEventType,
    @SerializedName("data")
    val data: ConnectionData,
    @SerializedName("msg")
    val message: String? = null
) : BaseSocketEvent(timeStamp = timeStamp, eventType = eventType)

@Keep
data class ConnectionData(
    @SerializedName("sid")
    val sessionId: String
)