package com.eka.conversation.data.local.db.entities

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.PrimaryKey
import com.eka.conversation.client.models.Message
import com.eka.conversation.common.Constants
import com.eka.conversation.data.local.db.entities.models.MessageRole
import com.eka.conversation.data.local.db.entities.models.MessageType
import com.eka.conversation.data.remote.socket.SocketEventSerializer
import com.eka.conversation.data.remote.socket.events.receive.ReceiveChatEvent
import com.eka.conversation.data.remote.socket.events.receive.StreamEvent
import com.eka.conversation.data.remote.socket.events.send.SendChatEvent
import com.google.gson.Gson

@Keep
@Entity(
    tableName = Constants.MESSAGES_TABLE_NAME,
    foreignKeys = [
        ForeignKey(
            entity = ChatSession::class,
            parentColumns = ["session_id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index(value = ["session_id"]),
    ]
)
data class MessageEntity(
    @PrimaryKey
    @ColumnInfo(name = "msg_id")
    val messageId: String,
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    @ColumnInfo(name = "role")
    val role: MessageRole,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "msg_type")
    val messageType: MessageType = MessageType.TEXT,
    @ColumnInfo(name = "content")
    val messageContent: String,
    @ColumnInfo(name = "owner_id", defaultValue = "owner_id_default")
    val ownerId: String? = "owner_id_default",
)

@Keep
@Fts4(contentEntity = MessageEntity::class)
@Entity(tableName = Constants.MESSAGES_FTS_TABLE_NAME)
data class MessageFTSEntity(
    @ColumnInfo(name = "msg_id") val messageId: String,
    @ColumnInfo(name = "session_id") val sessionId : String,
    @ColumnInfo(name = "content") val messageContent: String,
)

fun MessageEntity.toMessageModel(): Message? {
    return if (role == MessageRole.AI) {
        val socketEvent = SocketEventSerializer.deserializeReceivedEvent(data = messageContent)
        when (socketEvent) {
            is ReceiveChatEvent -> {
                when (messageType) {
                    MessageType.SINGLE_SELECT -> {
                        Message.SingleSelect(
                            messageId = messageId,
                            chatId = sessionId,
                            updatedAt = createdAt,
                            toolUseId = socketEvent.data?.toolUseId ?: "",
                            choices = socketEvent.data?.choices ?: emptyList()
                        )
                    }

                    MessageType.MULTI_SELECT -> {
                        Message.MultiSelect(
                            messageId = messageId,
                            chatId = sessionId,
                            updatedAt = createdAt,
                            toolUseId = socketEvent.data?.toolUseId ?: "",
                            choices = socketEvent.data?.choices ?: emptyList()
                        )
                    }

                    MessageType.TEXT -> {
                        Message.Text(
                            messageId = messageId,
                            chatId = sessionId,
                            role = role,
                            updatedAt = createdAt,
                            text = socketEvent.data?.text ?: ""
                        )
                    }
                }
            }

            is StreamEvent -> {
                Message.Text(
                    messageId = messageId,
                    chatId = sessionId,
                    role = role,
                    updatedAt = createdAt,
                    text = socketEvent.data.text ?: ""
                )
            }

            else -> {
                null
            }
        }
    } else {
        val event = Gson().fromJson(messageContent, SendChatEvent::class.java)
        return Message.Text(
            messageId = messageId,
            chatId = sessionId,
            role = role,
            updatedAt = createdAt,
            text = event.data.text ?: ""
        )
    }
}

