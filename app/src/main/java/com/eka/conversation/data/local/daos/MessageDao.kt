package com.eka.conversation.data.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.eka.conversation.common.Constants
import com.eka.conversation.data.local.db.entities.ChatSession
import com.eka.conversation.data.local.db.entities.MessageEntity
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

    @Query("SELECT session_id FROM ${Constants.MESSAGES_TABLE_NAME} LIMIT 1")
    suspend fun getSessionIdBySessionIdentity(): String?

    @Query("""
        SELECT * FROM ${Constants.MESSAGES_TABLE_NAME} WHERE (msg_id,session_id) IN (
            SELECT msg_id,session_id FROM ${Constants.MESSAGES_FTS_TABLE_NAME} WHERE content MATCH :query MATCH :query
        )
    """)
    fun searchMessages(query: String): Flow<List<MessageEntity>>

    @Query(
        """
        SELECT * FROM ${Constants.MESSAGES_TABLE_NAME} WHERE (msg_id,session_id) IN (
            SELECT msg_id,session_id FROM ${Constants.MESSAGES_FTS_TABLE_NAME} WHERE content MATCH :query MATCH :query
        ) AND owner_id = :ownerId
    """
    )
    fun searchMessagesWithOwnerId(query: String, ownerId: String): Flow<List<MessageEntity>>

    //     Get last messages for each session to show in session list
    @Query("""
        SELECT * FROM ${Constants.MESSAGES_TABLE_NAME} AS m1
        INNER JOIN (
            SELECT session_id, MAX(created_at) AS max_createdAt
            FROM ${Constants.MESSAGES_TABLE_NAME}
            GROUP BY session_id
        ) AS m2
        ON m1.session_id = m2.session_id AND m1.created_at = m2.max_createdAt ORDER BY m1.created_at DESC
    """
    )
    suspend fun getLastMessageOfEachSession(): List<MessageEntity>

    @Query(
        """
            SELECT * FROM ${Constants.MESSAGES_TABLE_NAME} AS m1
            INNER JOIN (
                SELECT session_id, MIN(created_at) AS min_createdAt
                FROM ${Constants.MESSAGES_TABLE_NAME}
                GROUP BY session_id
            ) AS m2
            ON m1.session_id = m2.session_id AND m1.created_at = m2.min_createdAt ORDER BY m1.created_at DESC
        """
    )
    suspend fun getFirstMessageOfEachSession(): List<MessageEntity>

    @Transaction
    suspend fun getAllSession(ownerId: String?): List<MessageEntity> {
        var lastMessages: List<MessageEntity> = emptyList()
        var firstMessages: List<MessageEntity> = emptyList()

        if (ownerId.isNullOrEmpty()) {
            lastMessages = getLastMessageOfEachSession()
            firstMessages = getFirstMessageOfEachSession()
        } else {
            lastMessages = getAllLastMessagesOfEachSessionWithFilter(ownerId = ownerId)
            firstMessages = getAllFirstMessagesOfEachSessionWithFilter(ownerId = ownerId)
        }

        val updateMap = lastMessages.associateBy { it.sessionId }

        val updatedList = firstMessages.map { item ->
            updateMap[item.sessionId]?.let { updatedItem ->
                item.copy(createdAt = updatedItem.createdAt)
            } ?: item
        }

        return updatedList
    }

    @Query(
        """
        SELECT * FROM ${Constants.MESSAGES_TABLE_NAME} AS m1
        INNER JOIN (
            SELECT session_id, MAX(created_at) AS max_createdAt 
            FROM ${Constants.MESSAGES_TABLE_NAME}
            WHERE owner_id = :ownerId
            GROUP BY session_id 
        ) AS m2 
        ON m1.session_id = m2.session_id AND m1.created_at = m2.max_createdAt ORDER BY m1.created_at DESC
    """
    )
    suspend fun getAllLastMessagesOfEachSessionWithFilter(ownerId: String): List<MessageEntity>

    @Query(
        """
        SELECT * FROM ${Constants.MESSAGES_TABLE_NAME} AS m1
        INNER JOIN (
            SELECT session_id, MIN(created_at) AS min_createdAt 
            FROM ${Constants.MESSAGES_TABLE_NAME}
            WHERE owner_id = :ownerId
            GROUP BY session_id 
        ) AS m2 
        ON m1.session_id = m2.session_id AND m1.created_at = m2.min_createdAt ORDER BY m1.created_at DESC
    """
    )
    suspend fun getAllFirstMessagesOfEachSessionWithFilter(ownerId: String): List<MessageEntity>

    // Get a message by its local ID
    @Query("SELECT * FROM ${Constants.MESSAGES_TABLE_NAME} WHERE msg_id = :msgId AND session_id = :sessionId")
    fun getMessageById(msgId: String, sessionId: String): MessageEntity?

    @Query("SELECT * FROM ${Constants.MESSAGES_TABLE_NAME}")
    fun getMessagesByContext(): List<MessageEntity>

    // Get messages by session id
    @Query("SELECT * FROM ${Constants.MESSAGES_TABLE_NAME} WHERE session_id = :sessionId ORDER BY created_at ASC")
    fun getMessagesBySessionId(sessionId : String): Flow<List<MessageEntity>>

    @Query("""UPDATE ${Constants.MESSAGES_TABLE_NAME} SET owner_id = :newOwnerId WHERE owner_id = "owner_id_default" """)
    fun updateAllMessagesWithNewOwnerId(newOwnerId: String)

    @Query("SELECT * FROM ${Constants.CHAT_SESSION} WHERE session_id = :sessionId")
    fun getChatSessionById(sessionId: String): ChatSession

    @Query("SELECT * FROM ${Constants.CHAT_SESSION} ORDER BY updated_at DESC LIMIT 1 ")
    fun getLastSessionData(): ChatSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertChatSession(chatSession: ChatSession)
}