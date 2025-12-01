package com.eka.conversation.internal

import com.eka.conversation.client.ChatSDK
import com.eka.conversation.common.ChatLogger
import com.eka.conversation.common.TimeUtils
import com.eka.conversation.data.local.db.entities.MessageEntity
import com.eka.conversation.data.local.db.entities.models.MessageRole
import com.eka.conversation.data.local.db.entities.models.MessageType
import com.eka.conversation.data.remote.socket.events.SocketContentType
import com.eka.conversation.data.remote.socket.events.receive.ReceiveChatEvent
import com.eka.conversation.domain.repositories.ChatRepository
import com.google.gson.Gson

internal object SocketEventHandler {
    suspend fun handleReceiveChatEvent(
        sessionId: String,
        receivedChatEvent: ReceiveChatEvent,
        chatRepository: ChatRepository,
    ) {
        val gson = Gson()
        when (receivedChatEvent.contentType) {
            SocketContentType.INLINE_TEXT -> {
                ChatSDK.provideSpeechToTextData(
                    result = Result.success(receivedChatEvent.data?.text)
                )
            }

            SocketContentType.SINGLE_SELECT -> {
                val message = chatRepository.getMessageById(
                    sessionId = sessionId,
                    messageId = receivedChatEvent.eventId
                )
                if (message == null) {
                    ChatLogger.d("SocketEventHandler", "Single Select Inserting new message")
                    chatRepository.insertMessages(
                        listOf(
                            MessageEntity(
                                messageType = MessageType.SINGLE_SELECT,
                                messageId = receivedChatEvent.eventId,
                                sessionId = sessionId,
                                role = MessageRole.AI,
                                createdAt = TimeUtils.getCurrentUTCEpochMillis(),
                                messageContent = gson.toJson(receivedChatEvent),
                                choices = receivedChatEvent.data?.choices,
                                toolUseId = receivedChatEvent.data?.toolUseId
                            )
                        )
                    )
                } else {
                    ChatLogger.d("SocketEventHandler", "Single Select Updating message $message")
                    chatRepository.updateMessage(
                        message.copy(
                            messageType = MessageType.SINGLE_SELECT,
                            choices = receivedChatEvent.data?.choices,
                            toolUseId = receivedChatEvent.data?.toolUseId
                        )
                    )
                }
            }

            SocketContentType.MULTI_SELECT -> {
                val message = chatRepository.getMessageById(
                    sessionId = sessionId,
                    messageId = receivedChatEvent.eventId
                )
                if (message == null) {
                    ChatLogger.d("SocketEventHandler", "Multiselect Inserting new message")
                    chatRepository.insertMessages(
                        listOf(
                            MessageEntity(
                                messageType = MessageType.MULTI_SELECT,
                                messageId = receivedChatEvent.eventId,
                                sessionId = sessionId,
                                role = MessageRole.AI,
                                createdAt = TimeUtils.getCurrentUTCEpochMillis(),
                                messageContent = gson.toJson(receivedChatEvent),
                                choices = receivedChatEvent.data?.choices,
                                toolUseId = receivedChatEvent.data?.toolUseId
                            )
                        )
                    )
                } else {
                    ChatLogger.d("SocketEventHandler", "Multiselect Updating message $message")
                    chatRepository.updateMessage(
                        message.copy(
                            messageType = MessageType.MULTI_SELECT,
                            choices = receivedChatEvent.data?.choices,
                            toolUseId = receivedChatEvent.data?.toolUseId
                        )
                    )
                }
            }

            SocketContentType.FILE -> {
                // TODO In this case upload a file and then store it in db
            }

            else -> {
                // This case will never happen for this type of event
            }
        }

    }
}