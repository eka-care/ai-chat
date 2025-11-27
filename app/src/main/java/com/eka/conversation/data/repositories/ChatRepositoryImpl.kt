package com.eka.conversation.data.repositories

import com.eka.conversation.client.models.ChatInfo
import com.eka.conversation.client.models.Message
import com.eka.conversation.common.Response
import com.eka.conversation.common.models.UserInfo
import com.eka.conversation.data.local.db.ChatDatabase
import com.eka.conversation.data.local.db.entities.ChatSession
import com.eka.conversation.data.local.db.entities.MessageEntity
import com.eka.conversation.data.local.db.entities.MessageFile
import com.eka.conversation.data.local.db.entities.toChatInfo
import com.eka.conversation.data.local.db.entities.toMessageModel
import com.eka.conversation.domain.repositories.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ChatRepositoryImpl(
    private val chatDatabase: ChatDatabase
) : ChatRepository {
    override suspend fun insertMessages(messages: List<MessageEntity>) {
        withContext(Dispatchers.IO) {
            try {
                chatDatabase.messageDao().insertMessages(messages = messages)
            } catch (_: Exception) {
            }
        }
    }

    override suspend fun updateMessage(message: MessageEntity) {
        withContext(Dispatchers.IO) {
            try {
                chatDatabase.messageDao().updateMessage(message = message)
            } catch (_: Exception) {
            }
        }
    }

    override suspend fun getLastSessionId(): Flow<String> {
        return withContext(Dispatchers.IO) {
            try {
                val response = chatDatabase.messageDao().getLastSessionId()
                response
            } catch (_: Exception) {
                flow {}
            }
        }
    }

    override fun getSearchResult(query: String): Flow<List<MessageEntity>> {
        try {
            val buildQuery = "*${query}*"
            val response = chatDatabase.messageDao().searchMessages(query = buildQuery)
            return response
        } catch (_: Exception) {
            return flow {}
        }
    }

    override fun getSearchResultWithOwnerId(
        query: String,
        ownerId: String
    ): Flow<List<MessageEntity>> {
        try {
            val buildQuery = "*${query}*"
            val response = chatDatabase.messageDao()
                .searchMessagesWithOwnerId(query = buildQuery, ownerId = ownerId)
            return response
        } catch (_: Exception) {
            return flow {}
        }
    }

    override fun getMessagesBySessionId(sessionId: String): Response<Flow<List<Message>>> {
        return try {
            val response = chatDatabase.messageDao().getMessagesBySessionId(sessionId = sessionId)
                .map { messageList -> messageList.mapNotNull { message -> message.toMessageModel() } }
            Response.Success(data = response)
        } catch (e: Exception) {
            Response.Error(message = e.message.toString())
        }
    }

    override suspend fun getMessageById(messageId: String, sessionId: String): MessageEntity? {
        return withContext(Dispatchers.IO) {
            try {
                val response = chatDatabase.messageDao()
                    .getMessageById(messageId = messageId, sessionId = sessionId)
                response
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun getLastMessagesOfEachSessionId(): Response<List<MessageEntity>> {
        return withContext(Dispatchers.IO) {
            try {
                val result = chatDatabase.messageDao().getAllSession(null)
                Response.Success(data = result)
            } catch (e: Exception) {
                Response.Error(message = e.message.toString())
            }
        }
    }

    override suspend fun getAllSession(ownerId: String?): Response<List<MessageEntity>> {
        return withContext(Dispatchers.IO) {
            try {
                val result = chatDatabase.messageDao().getAllSession(ownerId)
                Response.Success(data = result)
            } catch (e: Exception) {
                Response.Error(message = e.message.toString())
            }
        }
    }

    override suspend fun getMessagesByContext(chatContext: String): Response<List<MessageEntity>> {
        return withContext(Dispatchers.IO) {
            try {
                val result = chatDatabase.messageDao().getMessagesByContext()
                Response.Success(data = result)
            } catch (e: Exception) {
                Response.Error(message = e.message.toString())
            }
        }
    }

    override suspend fun fillPastMessagesWithOwnerId(ownerId: String) {
        withContext(Dispatchers.IO) {
            try {
                chatDatabase.messageDao().updateAllMessagesWithNewOwnerId(newOwnerId = ownerId)
            } catch (e: Exception) {
            }
        }
    }

    override suspend fun getLastMessagesOfEachSessionIdFilterByOwnerId(ownerId: String): Response<List<MessageEntity>> {
        return withContext(Dispatchers.IO) {
            try {
                val result = chatDatabase.messageDao()
                    .getAllLastMessagesOfEachSessionWithFilter(ownerId = ownerId)
                Response.Success(data = result)
            } catch (e: Exception) {
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
                    .getSessionIdBySessionIdentity()
                Response.Success(data = response)
            } catch (e: Exception) {
                Response.Error(message = e.message.toString())
            }
        }
    }

    override suspend fun getSessionData(sessionId: String): Result<ChatSession> =
        withContext(Dispatchers.IO) {
            try {
                val response = chatDatabase.messageDao().getChatSessionById(sessionId = sessionId)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getLastSession(userInfo: UserInfo?): Result<ChatInfo> =
        withContext(Dispatchers.IO) {
            try {
                val response = if (userInfo == null) {
                    chatDatabase.messageDao().getLastSession()
                } else {
                    chatDatabase.messageDao()
                        .getLastSessionData(
                            ownerId = userInfo.userId,
                            businessId = userInfo.businessId
                        )
                }
                if (response == null) return@withContext Result.failure(Exception("No session found for userId and BusinessId!"))
                Result.success(response.toChatInfo())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun insertChatSession(session: ChatSession): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                chatDatabase.messageDao().insertChatSession(session)
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}