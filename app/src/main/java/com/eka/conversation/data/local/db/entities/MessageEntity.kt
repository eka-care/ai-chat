package com.eka.conversation.data.local.db.entities

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.PrimaryKey
import com.eka.conversation.common.Constants
import com.eka.conversation.data.local.db.entities.models.MessageRole
import com.google.gson.annotations.SerializedName

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
    @SerializedName("msg_id")
    val msgId: String,
    @ColumnInfo(name = "session_id")
    @SerializedName("session_id")
    val sessionId: String,
    @ColumnInfo(name = "session_identity")
    @SerializedName("session_identity")
    val sessionIdentity: String? = null,
    @ColumnInfo(name = "role")
    @SerializedName("role")
    val role: MessageRole,
    @ColumnInfo(name = "message_files")
    @SerializedName("message_files")
    val messageFiles: List<String>? = null,
    @ColumnInfo(name = "message_text")
    @SerializedName("message_text")
    val messageText: String? = null,
    @ColumnInfo(name = "message_html_text")
    @SerializedName("message_html_text")
    val htmlString: String? = null,
    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    val createdAt: Long,
    @ColumnInfo(name = "chat_context")
    @SerializedName("chat_context")
    val chatContext: String? = null,
    @ColumnInfo(name = "chat_sub_context")
    @SerializedName("chat_sub_context")
    val chatSubContext: String? = null,
    @ColumnInfo(name = "chat_session_config")
    @SerializedName("chat_session_config")
    val chatSessionConfig: String? = null,
    @ColumnInfo(name = "msg_type")
    @SerializedName("msg_type")
    val msgType: String = "TEXT",
    @ColumnInfo(
        name = "owner_id",
        defaultValue = "owner_id_default"
    )
    @SerializedName("owner_id")
    val ownerId: String? = "owner_id_default",
)

@Keep
@Fts4(contentEntity = MessageEntity::class)
@Entity(tableName = Constants.MESSAGES_FTS_TABLE_NAME)
data class MessageFTSEntity(
    @ColumnInfo(name = "msg_id") val msgId: String,
    @ColumnInfo(name = "session_id") val sessionId : String,
    @ColumnInfo(name = "message_text") val messageText: String,
    @ColumnInfo(name = "chat_context") val chatContext: String? = null,
    @ColumnInfo(name = "chat_sub_context") val chatSubContext: String? = null,
)


