package com.eka.conversation.internal

import com.eka.conversation.client.ChatSDK
import com.eka.conversation.common.TimeUtils
import com.eka.conversation.data.local.db.entities.MessageEntity
import com.eka.conversation.data.local.db.entities.models.MessageRole
import com.eka.conversation.data.local.db.entities.models.MessageType
import com.eka.conversation.data.remote.socket.events.SocketContentType
import com.eka.conversation.data.remote.socket.events.receive.ReceiveChatEvent
import com.eka.conversation.domain.repositories.ChatRepository
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal object SocketEventHandler {
    fun handleReceiveChatEvent(
        sessionId: String,
        receivedChatEvent: ReceiveChatEvent,
        chatRepository: ChatRepository,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val gson = Gson()
            when (receivedChatEvent.contentType) {
                SocketContentType.INLINE_TEXT -> {
                    ChatSDK.provideSpeechToTextData(
                        result = Result.success(receivedChatEvent.data?.text)
                    )
                }

                SocketContentType.SINGLE_SELECT -> {
                    chatRepository.insertMessages(
                        listOf(
                            MessageEntity(
                                messageType = MessageType.SINGLE_SELECT,
                                messageId = receivedChatEvent.eventId,
                                sessionId = sessionId,
                                role = MessageRole.AI,
                                createdAt = TimeUtils.getCurrentUTCEpochMillis(),
                                messageContent = gson.toJson(receivedChatEvent)
                            )
                        )
                    )
                }

                SocketContentType.MULTI_SELECT -> {
                    chatRepository.insertMessages(
                        listOf(
                            MessageEntity(
                                messageType = MessageType.MULTI_SELECT,
                                messageId = receivedChatEvent.eventId,
                                sessionId = sessionId,
                                role = MessageRole.AI,
                                createdAt = TimeUtils.getCurrentUTCEpochMillis(),
                                messageContent = gson.toJson(receivedChatEvent)
                            )
                        )
                    )
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
}