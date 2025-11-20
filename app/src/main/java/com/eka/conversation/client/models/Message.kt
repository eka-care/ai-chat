package com.eka.conversation.client.models

import com.eka.conversation.data.local.db.entities.models.MessageRole

sealed class Message {
    data class Text(
        val msgId: String,
        val sessionId: String,
        val text: String,
        val role: MessageRole,
        val updatedAt: String
    ) : Message()

    data class MultiSelect(
        val msgId: String,
        val sessionId: String,
        val choices: List<String>,
        val toolUseId: String
    ) : Message()

    data class SingleSelect(
        val msgId: String,
        val sessionId: String,
        val toolUseId: String,
    )
}