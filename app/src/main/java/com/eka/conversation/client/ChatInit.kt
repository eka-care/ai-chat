package com.eka.conversation.client

import android.content.Context
import android.util.Log
import com.eka.conversation.client.interfaces.IChatSessionConfig
import com.eka.conversation.client.interfaces.IResponseStreamHandler
import com.eka.conversation.client.models.Message
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
        configuration = chatInitConfiguration
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
        try {
            EkaNetwork.init(
                networkConfig = chatInitConfiguration.networkConfig
            )
            database = ChatDatabase.getDatabase(context = context)
            database?.let {
                repository = ChatRepositoryImpl(it)
                sessionRepository = SessionManagementRepositoryImpl(
                    authConfiguration = chatInitConfiguration.authConfiguration
                )
                chatSessionManager = ChatSessionManager(
                    authConfiguration = chatInitConfiguration.authConfiguration,
                    sessionManagementRepository = sessionRepository!!,
                    chatRepository = repository!!
                )
            }
        } catch (e: Exception) {
            Log.e("ChatSDK", "ChatSDK initialization failed", e)
        }
        Log.d("ChatSDK", "ChatSDK initialized")
    }

    fun startSession(sessionId: String, chatSessionConfig: IChatSessionConfig) {
        CoroutineScope(Dispatchers.IO).launch {
            val session = repository?.getSessionData(sessionId = sessionId)?.getOrNull()
            if (session == null) {
                chatSessionConfig.onFailure(Exception("Session not found!"))
                return@launch
            }
            chatSessionManager?.startSession(
                sessionId = sessionId,
                chatSessionConfig = chatSessionConfig
            )
        }
    }

    fun startSession(chatSessionConfig: IChatSessionConfig) {
        CoroutineScope(Dispatchers.IO).launch {
            chatSessionManager?.startSession(chatSessionConfig = chatSessionConfig)
        }
    }

    fun sendNewQuery(toolUseId: String?, query: String, responseHandler: IResponseStreamHandler) {
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
        if (configuration == null) {
            throw IllegalStateException("Chat Init configuration not initialized")
        }
        return configuration!!
    }
}