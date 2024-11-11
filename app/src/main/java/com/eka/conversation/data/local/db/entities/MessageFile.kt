package com.eka.conversation.data.local.db.entities

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.eka.conversation.common.Constants
import com.eka.conversation.data.local.db.entities.models.MessageFileType

@Keep
@Entity(tableName = Constants.MESSAGES_FILES_TABLE_NAME)
data class MessageFile(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "local_file_id")
    val localFileId : Int,
    @ColumnInfo("message_file_name") val name: String? = null,
    @ColumnInfo("message_file_src") val src: String,
    @ColumnInfo("message_file_type") val fileType: MessageFileType = MessageFileType.ANY
)