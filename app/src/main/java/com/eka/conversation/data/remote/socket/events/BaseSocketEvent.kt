package com.eka.conversation.data.remote.socket.events

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
open class BaseSocketEvent(
    @SerializedName("timestamp")
    open val timeStamp: Long? = null,
    @SerializedName("event_type")
    open val eventType: SocketEventType
)