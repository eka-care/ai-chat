package com.eka.conversation.domain.repositories

import com.eka.conversation.client.models.ChatInfo
import com.eka.conversation.client.models.Message
import com.eka.conversation.common.Response
import com.eka.conversation.common.models.UserInfo
import com.eka.conversation.data.local.db.entities.ChatSession
import com.eka.conversation.data.local.db.entities.MessageEntity
import com.eka.conversation.data.local.db.entities.MessageFile
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    // local
    suspend fun insertMessages(messages : List<MessageEntity>)
    suspend fun updateMessage(message : MessageEntity)
    suspend fun getLastSessionId() : Flow<String>
    fun getSearchResult(query : String) : Flow<List<MessageEntity>>
    fun getSearchResultWithOwnerId(query: String, ownerId: String): Flow<List<MessageEntity>>
    fun getMessagesBySessionId(sessionId: String): Response<Flow<List<Message>>>
    suspend fun getMessageById(messageId: String, sessionId: String): MessageEntity?
    suspend fun getLastMessagesOfEachSessionId() : Response<List<MessageEntity>>

    suspend fun getAllSession(ownerId: String?): Response<List<MessageEntity>>

    suspend fun getMessagesByContext(chatContext: String): Response<List<MessageEntity>>
    suspend fun fillPastMessagesWithOwnerId(ownerId: String)
    suspend fun getLastMessagesOfEachSessionIdFilterByOwnerId(ownerId: String): Response<List<MessageEntity>>
    suspend fun deleteMessagesBySessionId(sessionId: String)
    suspend fun deleteMessageById(localMsgId: Int)

    suspend fun insertFiles(files : List<MessageFile>)
    suspend fun deleteFiles(files : List<MessageFile>) : Response<Boolean>
    suspend fun getFileById(fileId : Int) : Response<MessageFile>
    suspend fun getSessionIdBySessionIdentity(sessionIdentity: String): Response<String?>

    suspend fun getSessionData(sessionId: String): Result<ChatSession>
    suspend fun getLastSession(userInfo: UserInfo?): Result<ChatInfo>

    suspend fun insertChatSession(session: ChatSession): Result<Boolean>
}