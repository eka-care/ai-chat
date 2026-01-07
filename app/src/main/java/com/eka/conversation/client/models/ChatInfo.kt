package com.eka.conversation.client.models

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class ChatInfo(
    @SerializedName("session_id")
    val sessionId: String,
    @SerializedName("session_title")
    val sessionTitle: String?,
    @SerializedName("created_at")
    val createdAt: Long,
    @SerializedName("updated_at")
    val updatedAt: Long,
    @SerializedName("owner_id")
    val ownerId: String,
    @SerializedName("business_id")
    val businessId: String
)
