package com.eka.conversation.data.remote.socket.events

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class PongEvent(
    @SerializedName("ts")
    override val timeStamp: Long,
    @SerializedName("ev")
    override val eventType: SocketEventType,
) : BaseSocketEvent(timeStamp = timeStamp, eventType = eventType)
