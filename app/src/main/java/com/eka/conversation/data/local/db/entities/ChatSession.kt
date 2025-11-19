package com.eka.conversation.data.local.db.entities

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.eka.conversation.common.Constants
import com.google.gson.annotations.SerializedName

@Keep
@Entity(tableName = Constants.CHAT_SESSION)
data class ChatSession(
    @PrimaryKey
    @ColumnInfo(name = "session_id")
    @SerializedName("session_id")
    val sessionId: String,
    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    @SerializedName("updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "owner_id")
    @SerializedName("owner_id")
    val ownerId: String,
    @ColumnInfo(name = "business_id")
    @SerializedName("business_id")
    val businessId: String
)
