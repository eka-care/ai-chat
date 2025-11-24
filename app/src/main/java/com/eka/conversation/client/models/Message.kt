package com.eka.conversation.client.models

import com.eka.conversation.data.local.db.entities.models.MessageRole

sealed class Message(
    val msgId: String,
    val sessionId: String,
) {
    data class Text(
        val messageId: String,
        val chatId: String,
        val text: String,
        val role: MessageRole,
        val updatedAt: Long
    ) : Message(msgId = messageId, sessionId = chatId)

    data class MultiSelect(
        val messageId: String,
        val chatId: String,
        val choices: List<String>,
        val toolUseId: String,
        val updatedAt: Long
    ) : Message(msgId = messageId, sessionId = chatId)

    data class SingleSelect(
        val messageId: String,
        val chatId: String,
        val toolUseId: String,
        val updatedAt: Long,
        val choices: List<String>
    ) : Message(msgId = messageId, sessionId = chatId)
}