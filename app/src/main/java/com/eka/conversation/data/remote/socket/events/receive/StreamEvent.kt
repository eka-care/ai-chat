package com.eka.conversation.data.remote.socket.events.receive

import androidx.annotation.Keep
import com.eka.conversation.client.models.Message
import com.eka.conversation.data.local.db.entities.models.MessageRole
import com.eka.conversation.data.remote.socket.events.BaseSocketEvent
import com.eka.conversation.data.remote.socket.events.SocketContentType
import com.eka.conversation.data.remote.socket.events.SocketEventType
import com.google.gson.annotations.SerializedName

@Keep
data class StreamEvent(
    @SerializedName("ts")
    override val timeStamp: Long? = null,
    @SerializedName("ev")
    override val eventType: SocketEventType,
    @SerializedName("ct")
    val contentType: SocketContentType,
    @SerializedName("_id")
    val eventId: String,
    @SerializedName("data")
    val data: StreamData
) : BaseSocketEvent

@Keep
data class StreamData(
    @SerializedName("text")
    val text: String? = null,
    @SerializedName("progress_msg")
    val progressMsg: String? = null,
    @SerializedName("tips")
    val tips: List<String>? = null,
    @SerializedName("tool_id")
    val toolId: String? = null,
    @SerializedName("tool_name")
    val toolName: String? = null,
    @SerializedName("_meta")
    val meta: Map<String, Any>? = null
)

fun StreamEvent.toMessageModel(sessionId: String): Message {
    return Message.Text(
        messageId = eventId,
        chatId = sessionId,
        text = data.text ?: "",
        updatedAt = timeStamp ?: 0L,
        role = MessageRole.AI,
        toolUseId = null,
        choices = null
    )
}

