package com.eka.conversation.data.remote.models


import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class QueryResponseEvent(
    @SerializedName("msg_id")
    val messageId: Int?,
    @SerializedName("overwrite")
    val overwrite: Boolean?,
    @SerializedName("text")
    val text: String?,
    var isLastEvent: Boolean = false
)