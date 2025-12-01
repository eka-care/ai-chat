package com.eka.conversation.data.remote.socket.events.receive

import androidx.annotation.Keep
import com.eka.conversation.data.remote.socket.events.BaseSocketEvent
import com.eka.conversation.data.remote.socket.events.SocketEventType
import com.google.gson.annotations.SerializedName

@Keep
data class ErrorEvent(
    @SerializedName("ts")
    override val timeStamp: Long? = null,
    @SerializedName("ev")
    override val eventType: SocketEventType,
    @SerializedName("code")
    val code: String? = null,
    @SerializedName("msg")
    val message: String? = null,
) : BaseSocketEvent

enum class ErrorEventCode(val stringValue: String) {
    SESSION_EXPIRED("session_expired")
}