package com.eka.conversation.client

import android.content.Context
import com.eka.conversation.client.interfaces.IChatSessionConfig
import com.eka.conversation.client.interfaces.IResponseStreamHandler
import com.eka.conversation.client.models.Message
import com.eka.conversation.common.ChatLogger
import com.eka.conversation.common.Response
import com.eka.conversation.common.models.ChatInitConfiguration
import com.eka.conversation.data.local.db.ChatDatabase
import com.eka.conversation.data.repositories.ChatRepositoryImpl
import com.eka.conversation.data.repositories.SessionManagementRepositoryImpl
import com.eka.conversation.domain.repositories.ChatRepository
import com.eka.conversation.domain.repositories.SessionManagementRepository
import com.eka.conversation.internal.ChatSessionManager
import com.eka.networking.client.EkaNetwork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

object ChatInit {
    private var configuration: ChatInitConfiguration? = null
    private var database: ChatDatabase? = null
    private var repository: ChatRepository? = null
    private var sessionRepository: SessionManagementRepository? = null
    private var chatSessionManager: ChatSessionManager? = null

    fun initialize(
        chatInitConfiguration: ChatInitConfiguration,
        context: Context
    ) {
        val auth = chatInitConfiguration.authConfiguration
        require(auth.agentId.isNotBlank()) {
            throw IllegalStateException("Invalid auth configuration agentId is blank!")
        }
        require(auth.userId.isNotBlank()) {
            throw IllegalStateException("Invalid auth configuration userId is blank!")
        }
        require(auth.businessId.isNotBlank()) {
            throw IllegalStateException("Invalid auth configuration businessId is blank!")
        }
        configuration = chatInitConfiguration
        try {
            EkaNetwork.init(
                networkConfig = chatInitConfiguration.networkConfig
            )
            database = ChatDatabase.getDatabase(context = context)
            database?.let {
                repository = ChatRepositoryImpl(chatDatabase = it)
                sessionRepository = SessionManagementRepositoryImpl(
                    authConfiguration = chatInitConfiguration.authConfiguration
                )
            }
        } catch (e: Exception) {
            ChatLogger.e("ChatSDK", "ChatSDK initialisation failed", e)
        }
        ChatLogger.d("ChatSDK", "ChatSDK initialised")
    }

    fun initialiseChatSessionManager() {
        requireNotNull(sessionRepository) {
            throw IllegalStateException("ChatSDK not initialised!")
        }
        requireNotNull(repository) {
            throw IllegalStateException("ChatSDK not initialised!")
        }
        chatSessionManager?.cleanUp()
        chatSessionManager = null
        chatSessionManager = ChatSessionManager(
            authConfiguration = getChatInitConfiguration().authConfiguration,
            sessionManagementRepository = sessionRepository!!,
            chatRepository = repository!!
        )
    }

    fun startSession(sessionId: String, chatSessionConfig: IChatSessionConfig) {
        CoroutineScope(Dispatchers.IO).launch {
            val session = repository?.getSessionData(sessionId = sessionId)?.getOrNull()
            if (session == null) {
                chatSessionConfig.onFailure(Exception("Session not found!"))
                return@launch
            }
            initialiseChatSessionManager()
            chatSessionManager?.startSession(
                sessionId = sessionId,
                chatSessionConfig = chatSessionConfig
            )
        }
    }

    fun startSession(chatSessionConfig: IChatSessionConfig) {
        CoroutineScope(Dispatchers.IO).launch {
            initialiseChatSessionManager()
            chatSessionManager?.startSession(chatSessionConfig = chatSessionConfig)
        }
    }

    fun sendNewQuery(toolUseId: String?, query: String, responseHandler: IResponseStreamHandler) {
        requireNotNull(chatSessionManager) {
            throw IllegalStateException("Start Session not called before sending new query!")
        }
        chatSessionManager?.sendNewQuery(
            toolUseId = toolUseId,
            query = query,
            responseHandler = responseHandler
        )
    }

    fun getMessagesBySessionId(sessionId: String): Response<Flow<List<Message>>>? {
        return repository?.getMessagesBySessionId(sessionId)
    }

    fun getChatInitConfiguration(): ChatInitConfiguration {
        requireNotNull(configuration) {
            throw IllegalStateException("Chat Init configuration not initialized")
        }
        return configuration!!
    }
}