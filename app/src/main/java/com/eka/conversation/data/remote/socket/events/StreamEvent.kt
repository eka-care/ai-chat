package com.eka.conversation.data.remote.socket.events

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class StreamEvent(
    @SerializedName("ts")
    override val timeStamp: Long,
    @SerializedName("ev")
    override val eventType: SocketEventType,
    @SerializedName("ct")
    val contentType: SocketContentType,
    @SerializedName("_id")
    val eventId: String,
    @SerializedName("data")
    val data: StreamData
) : BaseSocketEvent(timeStamp = timeStamp, eventType = eventType)

//"audio": "Audio In ByteArray",
//"format": "audio/mp4"
@Keep
data class StreamData(
    @SerializedName("text")
    val text: String? = null,
    @SerializedName("audio")
    val audio: String? = null,
    @SerializedName("format")
    val format: String? = null
)

