package com.eka.conversation.data.remote.socket.events

import com.google.gson.annotations.SerializedName

data class SyncEvent(
    @SerializedName("ts")
    override val timeStamp: Long,
    @SerializedName("ev")
    override val eventType: SocketEventType,
    @SerializedName("_id")
    val eventId: String
) : BaseSocketEvent(timeStamp = timeStamp, eventType = eventType)
