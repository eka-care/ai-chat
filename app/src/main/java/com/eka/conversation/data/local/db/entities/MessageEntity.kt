package com.eka.conversation.data.local.db.entities

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.PrimaryKey
import com.eka.conversation.common.Constants
import com.eka.conversation.data.local.db.entities.models.MessageRole
import com.eka.conversation.data.local.db.entities.models.MessageType

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
    val msgId: String,
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    @ColumnInfo(name = "role")
    val role: MessageRole,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "msg_type")
    val msgType: MessageType = MessageType.TEXT,
    @ColumnInfo(name = "content")
    val msgContent: String,
    @ColumnInfo(name = "owner_id", defaultValue = "owner_id_default")
    val ownerId: String? = "owner_id_default",
)

@Keep
@Fts4(contentEntity = MessageEntity::class)
@Entity(tableName = Constants.MESSAGES_FTS_TABLE_NAME)
data class MessageFTSEntity(
    @ColumnInfo(name = "msg_id") val msgId: String,
    @ColumnInfo(name = "session_id") val sessionId : String,
    @ColumnInfo(name = "content") val msgContent: String,
)


