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
    @SerializedName("data")
    val data: ErrorEventData? = null,
    @SerializedName("code")
    private val codeRaw: String? = null,
    @SerializedName("msg")
    private val messageRaw: String? = null,
) : BaseSocketEvent {
    val code: String?
        get() = data?.code ?: codeRaw

    val message: String?
        get() = data?.msg ?: messageRaw
}

@Keep
data class ErrorEventData(
    @SerializedName("code")
    val code: String? = null,
    @SerializedName("msg")
    val msg: String? = null,
    @SerializedName("socket_code")
    val socketCode: Int? = null
)

enum class ErrorEventCode(val stringValue: String) {
    SESSION_EXPIRED("session_expired")
}