package com.eka.conversation.data.remote.socket.events

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
open class BaseSocketEvent(
    @SerializedName("ts")
    open val timeStamp: Long,
    @SerializedName("ev")
    open val eventType: SocketEventType
)