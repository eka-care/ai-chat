package com.eka.conversation.domain.repositories

import com.eka.conversation.common.Response
import com.eka.conversation.data.local.db.entities.MessageEntity
import com.eka.conversation.data.local.db.entities.MessageFile
import com.eka.conversation.data.remote.models.QueryPostRequest
import com.eka.conversation.data.remote.models.QueryPostResponse
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    // local
    suspend fun insertMessages(messages : List<MessageEntity>)
    suspend fun updateMessage(message : MessageEntity)
    suspend fun getLastSessionId() : Flow<String>
    fun getSearchResult(query : String) : Flow<List<MessageEntity>>
    fun getMessagesBySessionId(sessionId : String) : Response<Flow<List<MessageEntity>>>
    suspend fun getMessageById(msgId : Int,sessionId: String) : Response<Flow<MessageEntity>>
    suspend fun getLastMessagesOfEachSessionId() : Response<List<MessageEntity>>
    suspend fun deleteMessagesBySessionId(sessionId: String)
    suspend fun deleteMessageById(localMsgId: Int)

    suspend fun insertFiles(files : List<MessageFile>)
    suspend fun deleteFiles(files : List<MessageFile>) : Response<Boolean>
    suspend fun getFileById(fileId : Int) : Response<MessageFile>

    //remote
    suspend fun queryPost(queryPostRequest: QueryPostRequest) : Flow<String>
}