package com.eka.conversation.data.remote.socket.events

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class AuthEvent(
    @SerializedName("ts")
    override val timeStamp: Long,
    @SerializedName("ev")
    override val eventType: SocketEventType,
    @SerializedName("data")
    val data: AuthData,
    @SerializedName("_id")
    val eventId: String
) : BaseSocketEvent(timeStamp = timeStamp, eventType = eventType)

@Keep
data class AuthData(
    @SerializedName("token")
    val token: String
)
