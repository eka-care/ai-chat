package com.eka.conversation.data.remote.socket.events.send

import androidx.annotation.Keep
import com.eka.conversation.data.remote.socket.events.BaseSocketEvent
import com.eka.conversation.data.remote.socket.events.SocketEventType
import com.google.gson.annotations.SerializedName

@Keep
data class PingEvent(
    @SerializedName("ev")
    override val timeStamp: Long? = null,
    @SerializedName("ev")
    override val eventType: SocketEventType,
    @SerializedName("_id")
    val eventId: String
) : BaseSocketEvent