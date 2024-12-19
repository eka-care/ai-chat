package com.eka.conversation.data.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.eka.conversation.common.Constants
import com.eka.conversation.data.local.db.entities.MessageEntity
import com.eka.conversation.data.local.db.entities.models.MessageRole
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    // Insert a single message
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    // Insert multiple messages
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>): List<Long>

    // Update a specific message
    @Update
    suspend fun updateMessage(message: MessageEntity): Int

    // Get all messages
    @Query("SELECT * FROM ${Constants.MESSAGES_TABLE_NAME}")
    suspend fun getAllMessages(): List<MessageEntity>

    @Query("SELECT session_id FROM ${Constants.MESSAGES_TABLE_NAME} ORDER BY created_at DESC LIMIT 1")
    fun getLastSessionId() : Flow<String>

    @Query("""
        SELECT * FROM ${Constants.MESSAGES_TABLE_NAME} WHERE (msg_id,session_id) IN (
            SELECT msg_id,session_id FROM ${Constants.MESSAGES_FTS_TABLE_NAME} WHERE message_text MATCH :query or chat_context MATCH :query
        )
    """)
    fun searchMessages(query: String): Flow<List<MessageEntity>>

    // Get last messages for each session to show in session list
    @Query("""
        SELECT * FROM ${Constants.MESSAGES_TABLE_NAME} AS m1
        INNER JOIN (
            SELECT session_id, MAX(created_at) AS max_createdAt 
            FROM ${Constants.MESSAGES_TABLE_NAME}
            WHERE role = "USER"
            GROUP BY session_id 
        ) AS m2 
        ON m1.session_id = m2.session_id AND m1.created_at = m2.max_createdAt ORDER BY m1.created_at DESC
    """
    )
    suspend fun getAllLastMessagesOfEachSession() : List<MessageEntity>

    // Get a message by its local ID
    @Query("SELECT * FROM ${Constants.MESSAGES_TABLE_NAME} WHERE msg_id = :msgId AND session_id = :sessionId")
    fun getMessageById(msgId : Int, sessionId: String): Flow<MessageEntity>

    // Get messages by session id
    @Query("SELECT * FROM ${Constants.MESSAGES_TABLE_NAME} WHERE session_id = :sessionId ORDER BY created_at ASC")
    fun getMessagesBySessionId(sessionId : String): Flow<List<MessageEntity>>

    // Delete all messages
    @Query("DELETE FROM ${Constants.MESSAGES_TABLE_NAME}")
    suspend fun deleteAllMessages(): Int

    // Delete all messages by session id
    @Query("DELETE FROM ${Constants.MESSAGES_TABLE_NAME} WHERE session_id = :sessionId")
    suspend fun deleteMessagesBySessionId(sessionId: String): Int
}