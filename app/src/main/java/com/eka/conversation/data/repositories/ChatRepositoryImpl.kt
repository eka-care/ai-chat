package com.eka.conversation.data.repositories

import android.util.Log
import com.eka.conversation.ChatInit
import com.eka.conversation.common.Response
import com.eka.conversation.data.local.db.ChatDatabase
import com.eka.conversation.data.local.db.entities.MessageEntity
import com.eka.conversation.data.local.db.entities.MessageFile
import com.eka.conversation.data.remote.api.RetrofitClient
import com.eka.conversation.data.remote.models.QueryPostRequest
import com.eka.conversation.data.remote.models.QueryResponseEvent
import com.eka.conversation.domain.repositories.ChatRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class ChatRepositoryImpl(
    private val chatDatabase : ChatDatabase
) : ChatRepository{
    override suspend fun insertMessages(messages: List<MessageEntity>) {
        withContext(Dispatchers.IO) {
            try {
                chatDatabase.messageDao().insertMessages(messages = messages)
            }
            catch (_ : Exception) {
            }
        }
    }

    override suspend fun updateMessage(message: MessageEntity) {
        withContext(Dispatchers.IO) {
            try {
                chatDatabase.messageDao().updateMessage(message = message)
            }
            catch (_ : Exception) {
            }
        }
    }

    override suspend fun getLastSessionId(): Flow<String> {
        return withContext(Dispatchers.IO) {
            try {
                val response = chatDatabase.messageDao().getLastSessionId()
                response
            } catch (_ : Exception) {
                flow{}
            }
        }
    }

    override fun getSearchResult(query: String): Flow<List<MessageEntity>> {
        try {
            val buildQuery = "*${query}*"
            val response = chatDatabase.messageDao().searchMessages(buildQuery)
            return response
        } catch (_ : Exception) {
            return flow{}
        }
    }

    override fun getMessagesBySessionId(sessionId: String): Response<Flow<List<MessageEntity>>> {
        return try {
            val response = chatDatabase.messageDao().getMessagesBySessionId(sessionId = sessionId)
            Response.Success(data = response)
        } catch (e : Exception) {
            Response.Error(message = e.message.toString())
        }
    }

    override suspend fun getMessageById(msgId: Int, sessionId: String): Response<Flow<MessageEntity>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = chatDatabase.messageDao().getMessageById(msgId = msgId,sessionId = sessionId)
                if(response == null) {
                    Response.Error("Something went wrong!")
                } else {
                    Response.Success(data = response)
                }
            } catch (e : Exception) {
                Response.Error(message = e.message.toString())
            }
        }
    }

    override suspend fun getLastMessagesOfEachSessionId(): Response<List<MessageEntity>> {
        return withContext(Dispatchers.IO) {
            try {
                val res = chatDatabase.messageDao().getAllLastMessagesOfEachSession()
                Response.Success(data = res)
            } catch (e : Exception) {
                Response.Error(message = e.message.toString())
            }
        }
    }

    override suspend fun deleteMessagesBySessionId(sessionId: String) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteMessageById(localMsgId: Int) {
        TODO("Not yet implemented")
    }

    override suspend fun insertFiles(files: List<MessageFile>) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteFiles(files: List<MessageFile>): Response<Boolean> {
        TODO("Not yet implemented")
    }

    override suspend fun getFileById(fileId: Int): Response<MessageFile> {
        TODO("Not yet implemented")
    }

    override suspend fun getSessionIdBySessionIdentity(sessionIdentity: String): Response<String?> {
        return withContext(Dispatchers.IO) {
            try {
                val response = chatDatabase.messageDao()
                    .getSessionIdBySessionIdentity(sessionIdentity = sessionIdentity)
                Response.Success(data = response)
            } catch (e: Exception) {
                Response.Error(message = e.message.toString())
            }
        }
    }

    override suspend fun queryPost(queryPostRequest: QueryPostRequest): Flow<QueryResponseEvent> =
        flow {
        try {
            val networkConfiguration = ChatInit.getChatInitConfiguration().networkConfiguration
            val networkHeaders = networkConfiguration.headers
            networkHeaders.put("Accept","text/event-stream")
            networkHeaders.put("Content-Type","application/json")
            val url = networkConfiguration.baseUrl + networkConfiguration.aiBotEndpoint
            val res = RetrofitClient.chatApiService.postQuery(
                params = queryPostRequest.queryParams,
                body = queryPostRequest.body,
                headers = networkHeaders,
                url = url
            )

            var lastEventData: QueryResponseEvent? = null

            if(res.isSuccessful) {
                res.body()?.source()?.let { source ->
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line()
                        if (line != null && line.startsWith("data:")) {
                            val eventData = line.removePrefix("data:").trim()
                            val eventResponseData: QueryResponseEvent =
                                Gson().fromJson(eventData, QueryResponseEvent::class.java)
                            eventResponseData.isLastEvent = false
                            lastEventData = eventResponseData
                            lastEventData?.let {
                                emit(it)
                            }
                        }
                    }
                }
                lastEventData?.let {
                    it.isLastEvent = true
                    emit(it)
                }
            } else {
                emit(QueryResponseEvent(msgId = 0, overwrite = true, text = "", isLastEvent = true))
            }
        } catch (e : Exception) {
            emit(QueryResponseEvent(msgId = 0, overwrite = true, text = "", isLastEvent = true))
            Log.d("ChatRepo","Network Error: ${e.message.toString()}")
        }
    }.flowOn(Dispatchers.IO)
}