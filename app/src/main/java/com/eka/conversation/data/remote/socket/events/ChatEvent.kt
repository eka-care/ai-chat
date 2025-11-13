package com.eka.conversation.data.remote.socket.events

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

//"tool_use_id": "tooluse_oQTCjWV7T2K8hYOcnBw2qA",
//"choices": [
//"1-2 दिन से / 1-2 days",
//"3-5 दिन से / 3-5 days",
//"एक हफ्ते से ज्यादा / More than a week"
//],
//"additional_option": "none_of_the_above"

@Keep
data class ChatEvent(
    @SerializedName("ts")
    override val timeStamp: Long,
    @SerializedName("ev")
    override val eventType: SocketEventType,
    @SerializedName("data")
    val data: ChatData,
    @SerializedName("_id")
    val eventId: String,
    @SerializedName("ct")
    val contentType: SocketContentType
) : BaseSocketEvent(timeStamp = timeStamp, eventType = eventType)

@Keep
data class ChatData(
    @SerializedName("text")
    val text: String? = null,
    @SerializedName("tool_use_id")
    val toolUseId: String? = null,
    @SerializedName("choices")
    val choices: List<String>? = null,
    @SerializedName("additional_option")
    val additionalOption: String? = null,
    @SerializedName("extension")
    val extension: String? = null,
    @SerializedName("urls")
    val urls: List<String>? = null,
    @SerializedName("url")
    val url: String? = null,
)