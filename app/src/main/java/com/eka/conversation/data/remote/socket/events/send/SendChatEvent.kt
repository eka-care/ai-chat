package com.eka.conversation.data.remote.socket.events.send

import androidx.annotation.Keep
import com.eka.conversation.data.remote.socket.events.BaseSocketEvent
import com.eka.conversation.data.remote.socket.events.SocketContentType
import com.eka.conversation.data.remote.socket.events.SocketEventType
import com.google.gson.annotations.SerializedName

@Keep
data class SendChatEvent(
    @SerializedName("ts")
    override val timeStamp: Long? = null,
    @SerializedName("ev")
    override val eventType: SocketEventType,
    @SerializedName("data")
    val data: SendChatData,
    @SerializedName("_id")
    val eventId: String,
    @SerializedName("ct")
    val contentType: SocketContentType
) : BaseSocketEvent

@Keep
data class SendChatData(
    @SerializedName("text")
    val text: String? = null,
    @SerializedName("tool_use_id")
    val toolUseId: String? = null,
    @SerializedName("extension")
    val extension: String? = null,
    @SerializedName("url")
    val url: String? = null,
)