package com.eka.conversation.data.local.db.entities.models

import androidx.annotation.Keep
import com.eka.conversation.data.local.db.entities.MessageFile
import com.google.gson.annotations.SerializedName

@Keep
data class MessageContent(
    @SerializedName("message_role") val role: MessageRole,
    @SerializedName("message_text") val text: String? = null,
    @SerializedName("message_files") val messageFiles: List<MessageFile>? = null,
    @SerializedName("message_html") val htmlString: String? = null,
    val chatContext: String? = null,
    val createdAt: Long
)