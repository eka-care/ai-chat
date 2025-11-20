package com.eka.conversation.client

import com.eka.conversation.common.ChatLogger
import com.eka.conversation.common.Utils
import com.eka.conversation.common.models.AuthConfiguration
import com.eka.conversation.data.local.db.entities.MessageEntity
import com.eka.conversation.data.local.db.entities.models.MessageRole
import com.eka.conversation.data.local.db.entities.models.MessageType
import com.eka.conversation.data.local.preferences.ChatSharedPreferences
import com.eka.conversation.data.remote.models.responses.CreateSessionResponse
import com.eka.conversation.data.remote.models.responses.RefreshTokenResponse
import com.eka.conversation.data.remote.models.responses.SessionStatusResponse
import com.eka.conversation.data.remote.socket.SocketUtils
import com.eka.conversation.data.remote.socket.WebSocketManager
import com.eka.conversation.data.remote.socket.events.BaseSocketEvent
import com.eka.conversation.data.remote.socket.events.SocketContentType
import com.eka.conversation.data.remote.socket.events.SocketEventType
import com.eka.conversation.data.remote.socket.events.receive.ConnectionEvent
import com.eka.conversation.data.remote.socket.events.receive.EndOfStreamEvent
import com.eka.conversation.data.remote.socket.events.receive.ErrorEvent
import com.eka.conversation.data.remote.socket.events.receive.ReceiveChatEvent
import com.eka.conversation.data.remote.socket.events.receive.StreamEvent
import com.eka.conversation.data.remote.socket.events.send.SendChatData
import com.eka.conversation.data.remote.socket.events.send.SendChatEvent
import com.eka.conversation.data.remote.socket.states.SocketConnectionState
import com.eka.conversation.data.remote.socket.states.SocketMessage
import com.eka.conversation.domain.repositories.ChatRepository
import com.eka.conversation.domain.repositories.SessionManagementRepository
import com.google.gson.Gson
import com.haroldadmin.cnradapter.NetworkResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatSessionManager(
    private val chatPref: ChatSharedPreferences,
    private val authConfiguration: AuthConfiguration,
    private val sessionManagementRepository: SessionManagementRepository,
    private val chatRepository: ChatRepository
) {
    companion object {
        const val TAG = "ChatSessionManager"
    }

    val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _connectionState =
        MutableStateFlow<SocketConnectionState>(value = SocketConnectionState.Starting)

    private val _responseStream = MutableStateFlow<StreamEvent?>(value = null)

    private val _sendEnabled = MutableStateFlow<Boolean>(value = true)

    private var eventListenerJob: Job? = null

    private var socketManager: WebSocketManager? = null

    fun startExistingChatSession(userId: String) {
        coroutineScope.launch {
            val prevSessionId = chatPref.getString(ChatSharedPreferences.SESSION_ID)
            val prevSessionToken = chatPref.getString(ChatSharedPreferences.SESSION_TOKEN)
            if (prevSessionId.isNullOrBlank() || prevSessionToken.isNullOrBlank()) {
                startNewSession(userId = userId)
                return@launch
            }
            checkSessionActive(sessionId = prevSessionId).onSuccess {
                refreshSessionToken(
                    sessionId = prevSessionId,
                    prevSessToken = prevSessionToken
                ).onSuccess {
                    val newSessionToken = it.sessionToken
                    if (newSessionToken.isNullOrBlank()) {
                        _connectionState.value =
                            SocketConnectionState.Error(error = Exception("Error creating new session!"))
                        startNewSession(userId = userId)
                        return@launch
                    }
                    chatPref.setString(ChatSharedPreferences.SESSION_TOKEN, newSessionToken)
                    createSocketConnection(
                        sessionId = prevSessionId,
                        sessionToken = newSessionToken
                    )
                }.onFailure {
                    startNewSession(userId = userId)
                }
            }.onFailure { exception ->
                _connectionState.value =
                    SocketConnectionState.Error(error = Exception(exception))
                startNewSession(userId = userId)
            }
            return@launch
        }
    }

    fun sendEnabled() = _sendEnabled.asStateFlow()

    fun sendNewQuery(toolUseId: String?, query: String): Boolean {
        val chatQuery = SendChatEvent(
            timeStamp = Utils.getCurrentUTCEpochMillis(),
            eventType = SocketEventType.CHAT,
            eventId = Utils.getCurrentUTCEpochMillis().toString(),
            data = SendChatData(
                text = query,
                toolUseId = toolUseId
            ),
            contentType = SocketContentType.TEXT
        )
        val stringQuery = SocketUtils.sendEvent(chatQuery)
        if (stringQuery.isNullOrBlank()) {
            return false
        }
        val response = socketManager?.sendText(stringQuery) ?: false
        _sendEnabled.value = !response
        return response
    }

    fun startNewSession(userId: String) {
        coroutineScope.launch {
            createNewSession(userId = userId).onSuccess {
                val newSessionId = it.sessionId
                val newSessionToken = it.sessionToken
                if (newSessionId.isNullOrBlank() || newSessionToken.isNullOrBlank()) {
                    _connectionState.value =
                        SocketConnectionState.Error(error = Exception("Error creating new session!"))
                    return@launch
                }
                chatPref.setString(ChatSharedPreferences.SESSION_ID, newSessionId)
                chatPref.setString(ChatSharedPreferences.SESSION_TOKEN, newSessionToken)
                createSocketConnection(sessionId = newSessionId, sessionToken = newSessionToken)
            }.onFailure {
                _connectionState.value = SocketConnectionState.Error(error = Exception(it))
                return@launch
            }
        }
    }

    fun listenSocketEvents() {
        eventListenerJob?.cancel()
        eventListenerJob = null
        eventListenerJob = coroutineScope.launch {
            socketManager?.listenEvents()?.collect {
                handleSocketEvent(socketMessage = it)
            }
        }
    }

    fun handleSocketEvent(socketMessage: SocketMessage) {
        when (socketMessage) {
            is SocketMessage.TextMessage -> {
                val socketEvent = SocketUtils.buildReceiveEvent(data = socketMessage.text)
                storeSocketEventToDB(socketEvent)
            }

            is SocketMessage.ByteStringMessage -> {
                // Will not occur in any case
            }
        }
    }

    private fun storeSocketEventToDB(socketEvent: BaseSocketEvent?) {
        val sessionId = chatPref.getString(ChatSharedPreferences.SESSION_ID)
        if (sessionId == null) {
            return
        }
        when (socketEvent) {
            is ReceiveChatEvent -> {
                handleReceiveChatEvent(sessionId = sessionId, receivedChatEvent = socketEvent)
                ChatLogger.d(TAG, "ReceiveChatEvent $socketEvent")
            }

            is ConnectionEvent -> {
                _connectionState.value = SocketConnectionState.Connected
                ChatLogger.d(TAG, "ConnectionEvent $socketEvent")
            }

            is EndOfStreamEvent -> {
                val lastMessage = _responseStream.value
                lastMessage?.let {
                    handleStreamEvent(sessionId = sessionId, event = lastMessage)
                    _responseStream.value = null
                }
                _sendEnabled.value = true
                ChatLogger.d(TAG, "EndOfStreamEvent $socketEvent")
            }

            is StreamEvent -> {
                if (socketEvent.data.text.isNullOrBlank()) {
                    return
                }
                _responseStream.value = socketEvent
                ChatLogger.d(TAG, "StreamEvent $socketEvent")
            }

            is ErrorEvent -> {
                // TODO Retry session 3 times with new refresh token and then start new session
                ChatLogger.d(TAG, "ErrorEvent $socketEvent")
            }
        }
    }

    fun handleReceiveChatEvent(sessionId: String, receivedChatEvent: ReceiveChatEvent) {
        coroutineScope.launch {
            val gson = Gson()
            when (receivedChatEvent.contentType) {
                SocketContentType.INLINE_TEXT -> {
                    ChatInit.getChatInitConfiguration().speechToTextConfiguration.speechToText?.onSpeechToTextComplete(
                        receivedChatEvent.data?.text
                    )
                }

                SocketContentType.SINGLE_SELECT -> {
                    chatRepository.insertMessages(
                        listOf(
                            MessageEntity(
                                msgType = MessageType.SINGLE_SELECT,
                                msgId = receivedChatEvent.eventId,
                                sessionId = sessionId,
                                role = MessageRole.AI,
                                createdAt = Utils.getCurrentUTCEpochMillis(),
                                msgContent = gson.toJson(receivedChatEvent)
                            )
                        )
                    )
                }

                SocketContentType.MULTI_SELECT -> {
                    chatRepository.insertMessages(
                        listOf(
                            MessageEntity(
                                msgType = MessageType.MULTI_SELECT,
                                msgId = receivedChatEvent.eventId,
                                sessionId = sessionId,
                                role = MessageRole.AI,
                                createdAt = Utils.getCurrentUTCEpochMillis(),
                                msgContent = gson.toJson(receivedChatEvent)
                            )
                        )
                    )
                }

                SocketContentType.FILE -> {
                    // TODO In this case upload a file and then store it in db
                }

                else -> {
                    // This case will never happen for this type of event
                }
            }
        }
    }

    fun handleStreamEvent(sessionId: String, event: StreamEvent) {
        coroutineScope.launch {
            val msgType = when (event.contentType) {
                SocketContentType.SINGLE_SELECT -> MessageType.SINGLE_SELECT
                SocketContentType.MULTI_SELECT -> MessageType.MULTI_SELECT
                else -> MessageType.TEXT
            }
            chatRepository.insertMessages(
                listOf(
                    MessageEntity(
                        msgType = msgType,
                        msgId = event.eventId,
                        sessionId = sessionId,
                        role = MessageRole.AI,
                        createdAt = Utils.getCurrentUTCEpochMillis(),
                        msgContent = Gson().toJson(event)
                    )
                )
            )
        }
    }

    fun listenConnectionState() = _connectionState.asStateFlow()

    suspend fun createNewSession(userId: String): Result<CreateSessionResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = sessionManagementRepository.createNewSession(userId = userId)
                when (response) {
                    is NetworkResponse.Success -> {
                        ChatLogger.d(TAG, response.body.toString())
                        return@withContext Result.success(response.body)
                    }

                    is NetworkResponse.NetworkError -> {
                        ChatLogger.d(TAG, response.error.toString())
                        return@withContext Result.failure(Exception("Network Error!"))
                    }

                    is NetworkResponse.ServerError -> {
                        ChatLogger.d(TAG, response.error.toString())
                        return@withContext Result.failure(
                            Exception(
                                response.body?.error?.msg ?: "User Not Found!"
                            )
                        )
                    }

                    else -> {
                        ChatLogger.d(TAG, response.toString())
                        return@withContext Result.failure(Exception("Something went wrong!"))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext Result.failure(e)
            }
        }

    suspend fun refreshSessionToken(
        sessionId: String,
        prevSessToken: String
    ): Result<RefreshTokenResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response =
                    sessionManagementRepository.refreshSessionToken(
                        sessionId = sessionId,
                        prevSessToken = prevSessToken
                    )
                when (response) {
                    is NetworkResponse.Success -> {
                        ChatLogger.d(TAG, response.body.toString())
                        return@withContext Result.success(response.body)
                    }

                    is NetworkResponse.ServerError -> {
                        ChatLogger.d(TAG, response.body.toString())
                        return@withContext Result.failure(
                            Exception(
                                response.body?.error?.msg ?: "Session Not Found!"
                            )
                        )
                    }

                    is NetworkResponse.NetworkError -> {
                        ChatLogger.d(TAG, response.error.toString())
                        return@withContext Result.failure(Exception("Network Error!"))
                    }

                    else -> {
                        ChatLogger.d(TAG, response.toString())
                        return@withContext Result.failure(Exception("Something went wrong!"))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext Result.failure(e)
            }
        }

    suspend fun checkSessionActive(sessionId: String): Result<SessionStatusResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = sessionManagementRepository.checkSessionStatus(sessionId = sessionId)
                when (response) {
                    is NetworkResponse.Success -> {
                        ChatLogger.d(TAG, response.body.toString())
                        return@withContext Result.success(response.body)
                    }

                    is NetworkResponse.ServerError -> {
                        ChatLogger.d(TAG, response.body.toString())
                        return@withContext Result.failure(
                            Exception(
                                response.body?.error?.msg ?: "Session Not Found!"
                            )
                        )
                    }

                    is NetworkResponse.NetworkError -> {
                        ChatLogger.d(TAG, response.error.toString())
                        return@withContext Result.failure(Exception("Network Error!"))
                    }

                    else -> {
                        ChatLogger.d(TAG, response.toString())
                        return@withContext Result.failure(Exception("Something went wrong!"))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext Result.failure(e)
            }
        }

    fun createSocketConnection(sessionId: String, sessionToken: String) {
        socketManager = WebSocketManager.getInstance(
            url = buildSocketUrl(sessionId = sessionId),
            maxReconnectAttempts = 3,
            sessionToken = sessionToken,
            agentId = authConfiguration.agentId
        )
        listenSocketEvents()
        ChatLogger.d(TAG, buildSocketUrl(sessionId = sessionId))
        coroutineScope.launch {
            socketManager?.listenConnectionState()?.collect {
                _connectionState.value = it
            }
        }
        socketManager?.connect()
    }

    fun buildSocketUrl(sessionId: String): String {
        return if (ChatInit.getChatInitConfiguration().environment == Environment.PROD) {
            "wss://matrix-ws.eka.care/ws/med-assist/session/${sessionId}/"
        } else {
            "wss://matrix-ws.dev.eka.care/ws/med-assist/session/${sessionId}/"
        }
    }
}