package com.eka.conversation.internal

import com.eka.conversation.client.interfaces.IChatSessionConfig
import com.eka.conversation.client.interfaces.IResponseStreamHandler
import com.eka.conversation.common.ChatLogger
import com.eka.conversation.common.Utils
import com.eka.conversation.common.models.AuthConfiguration
import com.eka.conversation.data.local.db.entities.ChatSession
import com.eka.conversation.data.local.db.entities.MessageEntity
import com.eka.conversation.data.local.db.entities.models.MessageRole
import com.eka.conversation.data.local.db.entities.models.MessageType
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
import com.eka.conversation.data.remote.socket.events.receive.ErrorEventCode
import com.eka.conversation.data.remote.socket.events.receive.ReceiveChatEvent
import com.eka.conversation.data.remote.socket.events.receive.StreamEvent
import com.eka.conversation.data.remote.socket.events.receive.toMessageModel
import com.eka.conversation.data.remote.socket.events.send.SendChatData
import com.eka.conversation.data.remote.socket.events.send.SendChatEvent
import com.eka.conversation.data.remote.socket.states.SocketConnectionState
import com.eka.conversation.data.remote.socket.states.SocketMessage
import com.eka.conversation.data.remote.utils.UrlUtils.buildSocketUrl
import com.eka.conversation.domain.repositories.ChatRepository
import com.eka.conversation.domain.repositories.SessionManagementRepository
import com.eka.conversation.internal.EventHandler.handleReceiveChatEvent
import com.google.gson.Gson
import com.haroldadmin.cnradapter.NetworkResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class ChatSessionManager(
    private val authConfiguration: AuthConfiguration,
    private val sessionManagementRepository: SessionManagementRepository,
    private val chatRepository: ChatRepository
) {
    companion object {
        const val TAG = "ChatSessionManager"
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _connectionState =
        MutableStateFlow<SocketConnectionState>(value = SocketConnectionState.Starting)

    private val _responseStream = MutableStateFlow<StreamEvent?>(value = null)

    private val _sendEnabled = MutableStateFlow(value = true)

    private var eventListenerJob: Job? = null
    private var connectionListenerJob: Job? = null

    private var socketManager: WebSocketManager? = null
    private var currentSessionId: String? = null

    private fun responseStream() =
        _responseStream.asStateFlow().map { it?.toMessageModel(sessionId = currentSessionId ?: "") }

    private fun listenSocketEvents() {
        eventListenerJob?.cancel()
        eventListenerJob = null
        eventListenerJob = coroutineScope.launch {
            socketManager?.listenEvents()?.collect {
                handleSocketEvent(socketMessage = it)
            }
        }
    }

    private fun listenWebSocketConnectionEvent() {
        connectionListenerJob?.cancel()
        connectionListenerJob = null
        connectionListenerJob = coroutineScope.launch {
            socketManager?.listenConnectionState()?.collect {
                _connectionState.value = it
            }
        }
    }

    private fun handleSocketEvent(socketMessage: SocketMessage) {
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
        val sessionId = currentSessionId
        if (sessionId == null) {
            return
        }
        when (socketEvent) {
            is ReceiveChatEvent -> {
                handleReceiveChatEvent(
                    sessionId = sessionId,
                    receivedChatEvent = socketEvent,
                    chatRepository = chatRepository
                )
                ChatLogger.d(TAG, "ReceiveChatEvent $socketEvent")
            }

            is SendChatEvent -> {
                handleSendEvent(socketEvent)
                ChatLogger.d(TAG, "SendChatEvent $socketEvent")
            }

            is ConnectionEvent -> {
                _connectionState.value = SocketConnectionState.Connected
                ChatLogger.d(TAG, "ConnectionEvent $socketEvent")
            }

            is EndOfStreamEvent -> {
                handleEndOfStreamEvent(sessionId = sessionId)
                ChatLogger.d(TAG, "EndOfStreamEvent $socketEvent")
            }

            is StreamEvent -> {
                handleStreamResponse(socketEvent = socketEvent)
                ChatLogger.d(TAG, "StreamEvent $socketEvent")
            }

            is ErrorEvent -> {
                if (socketEvent.code == ErrorEventCode.SESSION_EXPIRED.stringValue) {
                    handleEndOfStreamEvent(sessionId = sessionId)
                    startSession(sessionId = sessionId)
                } else {
                    _connectionState.value =
                        SocketConnectionState.Error(error = Exception(socketEvent.message))
                }
                ChatLogger.d(TAG, "ErrorEvent $socketEvent")
            }
        }
    }

    private fun handleSendEvent(socketEvent: SendChatEvent) {
        coroutineScope.launch {
            val sessionId = currentSessionId
            if (sessionId.isNullOrBlank()) return@launch
            chatRepository.insertMessages(
                listOf(
                    MessageEntity(
                        msgType = MessageType.TEXT,
                        msgId = socketEvent.eventId,
                        sessionId = sessionId,
                        role = MessageRole.USER,
                        createdAt = Utils.getCurrentUTCEpochMillis(),
                        msgContent = Gson().toJson(socketEvent)
                    )
                )
            )
        }
    }

    private fun handleEndOfStreamEvent(sessionId: String) {
        val lastMessage = _responseStream.value
        lastMessage?.let {
            handleStreamEvent(sessionId = sessionId, event = lastMessage)
            _responseStream.value = null
        }
        _sendEnabled.value = true
    }

    private fun handleStreamResponse(
        socketEvent: StreamEvent
    ) {
        if (socketEvent.data.text.isNullOrBlank()) {
            return
        }
        val newResponseText = _responseStream.value?.data?.text ?: ""
        _responseStream.value = socketEvent.copy(
            data = socketEvent.data.copy(
                text = newResponseText + socketEvent.data.text
            )
        )
    }

    private fun handleStreamEvent(sessionId: String, event: StreamEvent) {
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

    private suspend fun createNewSession(userId: String): Result<CreateSessionResponse> =
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

    private suspend fun refreshSessionToken(
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

    private suspend fun checkSessionActive(sessionId: String): Result<SessionStatusResponse> =
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

    private fun createSocketConnection(
        sessionId: String,
        sessionToken: String,
        chatSessionConfig: IChatSessionConfig? = null
    ) {
        socketManager = WebSocketManager.Companion.getInstance(
            url = buildSocketUrl(sessionId = sessionId),
            maxReconnectAttempts = 3,
            sessionToken = sessionToken,
            agentId = authConfiguration.agentId
        )
        listenSocketEvents()
        listenWebSocketConnectionEvent()
        ChatLogger.d(TAG, buildSocketUrl(sessionId = sessionId))
        currentSessionId = sessionId
        chatSessionConfig?.onSuccess(
            sessionId = sessionId,
            connectionState = _connectionState.asStateFlow(),
            sessionMessages = chatRepository.getMessagesBySessionId(sessionId = sessionId),
            queryEnabled = _sendEnabled.asStateFlow()
        )
        socketManager?.connect()
    }

    fun startSession(sessionId: String, chatSessionConfig: IChatSessionConfig? = null) {
        coroutineScope.launch {
            val chatSession = chatRepository.getSessionData(sessionId = sessionId).getOrNull()
            if (chatSession == null) {
                _connectionState.value =
                    SocketConnectionState.Error(error = Exception("Session not found!"))
                chatSessionConfig?.onFailure(error = Exception("Session not found!"))
                return@launch
            }

            val prevSessionId = chatSession.sessionId
            val prevSessionToken = chatSession.sessionToken
            if (prevSessionId.isBlank() || prevSessionToken.isBlank()) {
                _connectionState.value =
                    SocketConnectionState.Error(error = Exception("Session not valid!"))
                chatSessionConfig?.onFailure(error = Exception("Session not valid!"))
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
                            SocketConnectionState.Error(error = Exception("Session Expired!"))
                        return@launch
                    }
                    chatRepository.insertChatSession(
                        session = chatSession.copy(
                            sessionToken = newSessionToken,
                            updatedAt = Utils.getCurrentUTCEpochMillis()
                        )
                    )
                    createSocketConnection(
                        sessionId = prevSessionId,
                        sessionToken = newSessionToken,
                        chatSessionConfig = chatSessionConfig
                    )
                }.onFailure {
                    _connectionState.value = SocketConnectionState.Error(error = Exception(it))
                }
            }.onFailure { exception ->
                _connectionState.value =
                    SocketConnectionState.Error(error = Exception(exception))
            }
            return@launch
        }
    }


    fun startSession(chatSessionConfig: IChatSessionConfig? = null) {
        coroutineScope.launch {
            createNewSession(userId = authConfiguration.userId).onSuccess {
                val newSessionId = it.sessionId
                val newSessionToken = it.sessionToken
                if (newSessionId.isNullOrBlank() || newSessionToken.isNullOrBlank()) {
                    _connectionState.value =
                        SocketConnectionState.Error(error = Exception("Error creating new session!"))
                    return@launch
                }
                chatRepository.insertChatSession(
                    ChatSession(
                        sessionId = newSessionId,
                        sessionToken = newSessionToken,
                        createdAt = Utils.getCurrentUTCEpochMillis(),
                        updatedAt = Utils.getCurrentUTCEpochMillis(),
                        ownerId = authConfiguration.userId,
                        businessId = authConfiguration.businessId
                    )
                )
                createSocketConnection(
                    sessionId = newSessionId,
                    sessionToken = newSessionToken,
                    chatSessionConfig = chatSessionConfig
                )
            }.onFailure {
                chatSessionConfig?.onFailure(Exception(it))
                _connectionState.value = SocketConnectionState.Error(error = Exception(it))
                return@launch
            }
        }
    }

    fun sendNewQuery(
        toolUseId: String?,
        query: String,
        responseHandler: IResponseStreamHandler
    ): Boolean {
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
        _responseStream.value = null
        val response = socketManager?.sendText(stringQuery) ?: false
        if (response) {
            responseHandler.onSuccess(responseStream = responseStream())
            storeSocketEventToDB(socketEvent = chatQuery)
        } else {
            responseHandler.onFailure(Exception("Error sending query!"))
        }
        _sendEnabled.value = !response
        return response
    }
}