package com.eka.conversation.data.local.db.entities

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.eka.conversation.common.Constants

@Keep
@Entity(tableName = Constants.CHAT_SESSION)
data class ChatSession(
    @PrimaryKey
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    @ColumnInfo(name = "session_token")
    val sessionToken: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "owner_id")
    val ownerId: String,
    @ColumnInfo(name = "business_id")
    val businessId: String
)
