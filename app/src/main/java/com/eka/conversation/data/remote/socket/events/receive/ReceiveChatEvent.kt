package com.eka.conversation.data.remote.socket.events.receive

import androidx.annotation.Keep
import com.eka.conversation.data.remote.socket.events.BaseSocketEvent
import com.eka.conversation.data.remote.socket.events.SocketContentType
import com.eka.conversation.data.remote.socket.events.SocketEventType
import com.google.gson.annotations.SerializedName

@Keep
data class ReceiveChatEvent(
    @SerializedName("ts")
    override val timeStamp: Long? = null,
    @SerializedName("ev")
    override val eventType: SocketEventType,
    @SerializedName("data")
    val data: ReceiveChatData? = null,
    @SerializedName("_id")
    val eventId: String,
    @SerializedName("ct")
    val contentType: SocketContentType
) : BaseSocketEvent

@Keep
data class ReceiveChatData(
    @SerializedName("text")
    val text: String? = null,
    @SerializedName("tool_use_id")
    val toolUseId: String? = null,
    @SerializedName("choices")
    val choices: List<String>? = null,
    @SerializedName("additional_option")
    val additionalOption: String? = null,
    @SerializedName("urls")
    val urls: List<String>? = null,
)

//
//@Keep
//data class SendChatData(
//    @SerializedName("text")
//    val text: String? = null,
//    @SerializedName("tool_use_id")
//    val toolUseId: String? = null,
//    @SerializedName("extension")
//    val extension: String? = null,
//    @SerializedName("url")
//    val url: String? = null,
//)