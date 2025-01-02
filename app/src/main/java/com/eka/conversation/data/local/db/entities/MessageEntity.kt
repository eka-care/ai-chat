package com.eka.conversation.data.local.db.entities

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import com.eka.conversation.common.Constants
import com.eka.conversation.data.local.db.entities.models.MessageRole

@Keep
@Entity(tableName = Constants.MESSAGES_TABLE_NAME,
    primaryKeys = ["msg_id","session_id"])
data class MessageEntity(
    @ColumnInfo(name = "msg_id") val msgId : Int,
    @ColumnInfo(name = "session_id") val sessionId : String,
    @ColumnInfo(name = "session_identity") val sessionIdentity: String? = null,
    @ColumnInfo(name = "role") val role : MessageRole,
    @ColumnInfo(name = "message_files") val messageFiles : List<Int>? = null,
    @ColumnInfo(name = "message_text") val messageText : String? = null,
    @ColumnInfo(name = "message_html_text") val htmlString : String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "chat_context") val chatContext: String? = null,
    @ColumnInfo(name = "chat_sub_context") val chatSubContext: String? = null,
    @ColumnInfo(name = "chat_session_config") val chatSessionConfig: String? = null,
    @ColumnInfo(
        name = "owner_id",
        defaultValue = "owner_id_default"
    ) val ownerId: String? = "owner_id_default",
)

@Keep
@Fts4(contentEntity = MessageEntity::class)
@Entity(tableName = Constants.MESSAGES_FTS_TABLE_NAME)
data class MessageFTSEntity(
    @ColumnInfo(name = "msg_id") val msgId : Int,
    @ColumnInfo(name = "session_id") val sessionId : String,
    @ColumnInfo(name = "message_text") val messageText: String,
    @ColumnInfo(name = "chat_context") val chatContext: String? = null,
    @ColumnInfo(name = "chat_sub_context") val chatSubContext: String? = null,
)


