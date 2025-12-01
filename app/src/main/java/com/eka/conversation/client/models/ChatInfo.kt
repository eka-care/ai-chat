package com.eka.conversation.client.models

import androidx.annotation.Keep

@Keep
data class ChatInfo(
    val sessionId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val ownerId: String,
    val businessId: String
)
