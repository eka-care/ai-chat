package com.eka.conversation.data.remote.socket.events.send

import androidx.annotation.Keep
import com.eka.conversation.data.remote.socket.events.BaseSocketEvent
import com.eka.conversation.data.remote.socket.events.SocketContentType
import com.eka.conversation.data.remote.socket.events.SocketEventType
import com.google.gson.annotations.SerializedName

@Keep
data class SendStreamEvent(
    @SerializedName("ts")
    override val timeStamp: Long? = null,
    @SerializedName("ev")
    override val eventType: SocketEventType,
    @SerializedName("ct")
    val contentType: SocketContentType,
    @SerializedName("_id")
    val eventId: String,
    @SerializedName("data")
    val data: SendStreamData
) : BaseSocketEvent

//"audio": "Audio In ByteArray",
//"format": "audio/mp4"

@Keep
data class SendStreamData(
    @SerializedName("audio")
    val audio: String? = null,
    @SerializedName("format")
    val format: String? = null
)