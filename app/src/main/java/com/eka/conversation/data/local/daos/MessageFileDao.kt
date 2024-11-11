package com.eka.conversation.data.local.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.eka.conversation.common.Constants
import com.eka.conversation.data.local.db.entities.MessageFile

@Dao
interface MessageFileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessageFile(messageFile: MessageFile): Long

    // Insert multiple MessageFiles
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessageFiles(messageFiles: List<MessageFile>): List<Long>

    // Delete a single MessageFile
    @Delete
    suspend fun deleteMessageFile(messageFile: MessageFile)

    // Delete multiple MessageFiles
    @Delete
    suspend fun deleteMessageFiles(messageFiles: List<MessageFile>)

    @Query("DELETE FROM ${Constants.MESSAGES_FILES_TABLE_NAME}")
    suspend fun deleteAllMessageFiles()

    // Retrieve a single MessageFile by its localFileId
    @Query("SELECT * FROM ${Constants.MESSAGES_FILES_TABLE_NAME} WHERE local_file_id = :fileId")
    suspend fun getMessageFileById(fileId: Int): MessageFile?
}