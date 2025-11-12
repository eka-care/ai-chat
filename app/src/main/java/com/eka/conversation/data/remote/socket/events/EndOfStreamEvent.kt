package com.eka.conversation.data.remote.socket.events

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class EndOfStreamEvent(
    @SerializedName("ts")
    override val timeStamp: Long,
    @SerializedName("ev")
    override val eventType: SocketEventType,
    @SerializedName("_id")
    val eventId: String,
    @SerializedName("ct")
    val contentType: SocketContentType
) : BaseSocketEvent(timeStamp = timeStamp, eventType = eventType)
