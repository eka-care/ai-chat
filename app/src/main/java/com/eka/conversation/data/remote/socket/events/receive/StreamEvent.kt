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
) : BaseSocketEvent(timeStamp = timeStamp, eventType = eventType)

@Keep
data class StreamData(
    @SerializedName("text")
    val text: String? = null
)

fun StreamEvent.toMessageModel(sessionId: String): Message {
    return Message.Text(
        msgId = eventId,
        sessionId = sessionId,
        text = data.text ?: "",
        updatedAt = timeStamp ?: 0L,
        role = MessageRole.AI
    )
}

