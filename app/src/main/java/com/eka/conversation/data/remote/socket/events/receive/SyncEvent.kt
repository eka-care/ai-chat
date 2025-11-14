package com.eka.conversation.data.remote.socket.events.receive

import com.eka.conversation.data.remote.socket.events.BaseSocketEvent
import com.eka.conversation.data.remote.socket.events.SocketEventType
import com.google.gson.annotations.SerializedName

data class SyncEvent(
    @SerializedName("ts")
    override val timeStamp: Long? = null,
    @SerializedName("ev")
    override val eventType: SocketEventType,
    @SerializedName("_id")
    val eventId: String
) : BaseSocketEvent(timeStamp = timeStamp, eventType = eventType)