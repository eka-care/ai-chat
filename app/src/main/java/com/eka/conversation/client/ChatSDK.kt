package com.eka.conversation.client

import android.content.Context
import com.eka.conversation.client.events.SDKEventListener
import com.eka.conversation.client.events.SDKEventLogger
import com.eka.conversation.client.events.SDKEventType
import com.eka.conversation.client.interfaces.ResponseStreamCallback
import com.eka.conversation.client.interfaces.SessionCallback
import com.eka.conversation.client.models.ChatInfo
import com.eka.conversation.client.models.Message
import com.eka.conversation.common.ChatLogger
import com.eka.conversation.common.Response
import com.eka.conversation.common.models.ChatConfiguration
import com.eka.conversation.common.models.SpeechToTextConfiguration
import com.eka.conversation.common.models.UserInfo
import com.eka.conversation.data.local.db.ChatDatabase
import com.eka.conversation.data.remote.socket.models.AudioFormat
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

object ChatSDK {
    private var configuration: ChatConfiguration? = null
    private var database: ChatDatabase? = null
    private var repository: ChatRepository? = null
    private var sessionRepository: SessionManagementRepository? = null
    private var chatSessionManager: ChatSessionManager? = null

    private var speechToTextConfiguration: SpeechToTextConfiguration? = null

    private fun attachSpeechToTextConfig(speechToTextConfiguration: SpeechToTextConfiguration) {
        this.speechToTextConfiguration = speechToTextConfiguration
    }

    internal fun provideSpeechToTextData(result: Result<String?>) {
        speechToTextConfiguration?.speechToText?.onSpeechToTextComplete(result = result)
    }

    fun initialize(
        chatConfiguration: ChatConfiguration,
        context: Context
    ) {
        val auth = chatConfiguration.authConfiguration
        require(auth.agentId.isNotBlank()) {
            SDKEventLogger.error(SDKEventType.SDK) {
                put("error", "Invalid auth configuration agentId is blank!")
            }
            throw IllegalStateException("Invalid auth configuration agentId is blank!")
        }
        configuration = chatConfiguration
        try {
            EkaNetwork.init(
                networkConfig = chatConfiguration.networkConfig
            )
            database = ChatDatabase.getDatabase(context = context.applicationContext)
            database?.let {
                repository = ChatRepositoryImpl(chatDatabase = it)
                sessionRepository = SessionManagementRepositoryImpl(
                    authConfiguration = chatConfiguration.authConfiguration
                )
            }
            ChatLogger.d("ChatSDK", "ChatSDK initialised")
            SDKEventLogger.info(SDKEventType.SDK) {
                put("event", "sdk_initialized")
                put("agentId", auth.agentId)
            }
        } catch (e: Exception) {
            ChatLogger.e("ChatSDK", "ChatSDK initialisation failed", e)
            SDKEventLogger.error(SDKEventType.SDK) {
                put("event", "sdk_initialization_failed")
                put("error", e.message ?: "Unknown error")
                put("errorType", e.javaClass.simpleName)
            }
        }
    }

    fun initializeSessionManager() {
        requireNotNull(sessionRepository) {
            SDKEventLogger.error(SDKEventType.SESSION_MANAGEMENT) {
                put("error", "Session repository not initialised!")
            }
            throw IllegalStateException("ChatSDK not initialised!")
        }
        requireNotNull(repository) {
            SDKEventLogger.error(SDKEventType.SESSION_MANAGEMENT) {
                put("error", "chat_repository_not_initialised")
            }
            throw IllegalStateException("ChatSDK not initialised!")
        }
        chatSessionManager?.cleanUp()
        chatSessionManager = null
        chatSessionManager = ChatSessionManager(
            authConfiguration = getChatConfiguration().authConfiguration,
            sessionManagementRepository = sessionRepository!!,
            chatRepository = repository!!
        )
    }

    fun convertAudioToText(
        audioFilePath: String,
        audioFormat: AudioFormat,
        speechToTextConfiguration: SpeechToTextConfiguration
    ) {
        attachSpeechToTextConfig(speechToTextConfiguration = speechToTextConfiguration)
        chatSessionManager?.convertAudioToText(
            audioFilePath = audioFilePath,
            audioFormat = audioFormat
        )
    }

    suspend fun getLastSession(userInfo: UserInfo?): Result<ChatInfo>? {
        return repository?.getLastSession(userInfo = userInfo)
    }


    fun startSession(sessionId: String, callback: SessionCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            val session = repository?.getSessionData(sessionId = sessionId)?.getOrNull()
            if (session == null) {
                callback.onFailure(Exception("Session not found!"))
                return@launch
            }
            initializeSessionManager()
            chatSessionManager?.startSession(
                sessionId = sessionId,
                chatSessionConfig = callback
            )
        }
    }

    fun startSession(userInfo: UserInfo, callback: SessionCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            require(userInfo.userId.isNotBlank()) {
                SDKEventLogger.error(SDKEventType.SESSION_MANAGEMENT) {
                    put("error", "Invalid user info userId is blank!")
                }
                throw IllegalStateException("Invalid user info userId is blank!")
            }
            require(userInfo.businessId.isNotBlank()) {
                SDKEventLogger.error(SDKEventType.SESSION_MANAGEMENT) {
                    put("error", "Invalid user info businessId is blank!")
                }
                throw IllegalStateException("Invalid user info businessId is blank!")
            }
            initializeSessionManager()
            chatSessionManager?.startSession(
                userInfo = userInfo,
                chatSessionConfig = callback
            )
        }
    }

    fun sendQuery(toolUseId: String?, query: String, callback: ResponseStreamCallback) {
        requireNotNull(chatSessionManager) {
            SDKEventLogger.error(SDKEventType.MESSAGE) {
                put("error", "Start Session not called before sending query!")
            }
            throw IllegalStateException("Start Session not called before sending query!")
        }
        chatSessionManager?.sendNewQuery(
            toolUseId = toolUseId,
            query = query,
            responseHandler = callback
        )
    }

    fun getMessages(sessionId: String): Response<Flow<List<Message>>>? {
        return repository?.getMessagesBySessionId(sessionId)
    }

    fun getChatConfiguration(): ChatConfiguration {
        requireNotNull(configuration) {
            throw IllegalStateException("Chat configuration not initialized")
        }
        return configuration!!
    }

    /**
     * Add an event listener to receive SDK events
     * @param listener The listener that will receive SDK events
     */
    fun addEventListener(listener: SDKEventListener) {
        SDKEventLogger.addListener(listener)
    }

    /**
     * Remove an event listener
     * @param listener The listener to remove
     */
    fun removeEventListener(listener: SDKEventListener) {
        SDKEventLogger.removeListener(listener)
    }

    /**
     * Remove all event listeners
     */
    fun removeAllEventListeners() {
        SDKEventLogger.removeAllListeners()
    }

    /**
     * Enable or disable SDK event logging
     * @param enabled true to enable, false to disable
     */
    fun setEventLoggingEnabled(enabled: Boolean) {
        SDKEventLogger.setEnabled(enabled)
    }
}