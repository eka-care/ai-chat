package com.eka.conversation.ui.presentation.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eka.conversation.common.Response
import com.eka.conversation.common.Utils
import com.eka.conversation.data.local.db.ChatDatabase
import com.eka.conversation.data.local.db.entities.MessageEntity
import com.eka.conversation.data.local.db.entities.models.MessageRole
import com.eka.conversation.data.remote.models.QueryPostBody
import com.eka.conversation.data.remote.models.QueryPostRequest
import com.eka.conversation.data.remote.models.QueryResponseEvent
import com.eka.conversation.data.repositories.ChatRepositoryImpl
import com.eka.conversation.domain.repositories.ChatRepository
import com.eka.conversation.ui.presentation.states.ChatUiState
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch

class ChatViewModel(
    app : Application
) : AndroidViewModel(app) {

    companion object {
        lateinit var database : ChatDatabase
        lateinit var chatRepository: ChatRepository
    }

    init {
        database = ChatDatabase.getDatabase(app)
        chatRepository = ChatRepositoryImpl(database)
    }

    private val _chatScreenUiState = MutableStateFlow<ChatUiState>(ChatUiState.ChatInitLoading)
    val chatScreenUiState = _chatScreenUiState.asStateFlow()

    private val _lastMessagesSession = MutableStateFlow<List<MessageEntity>?>(null)
    val lastMessagesSession = _lastMessagesSession.asStateFlow()

    private val _currentSessionId = MutableStateFlow<String>("")
    val currentSessionId = _currentSessionId.asStateFlow()

    private val _updateCurrentSessionId = MutableStateFlow<Boolean>(true)
    val updateCurrentSessionId = _updateCurrentSessionId.asStateFlow()

    private val _lastQueryResponse = MutableStateFlow<Response<Boolean>>(Response.Loading())
    val lastQueryResponse = _lastQueryResponse.asStateFlow()

    var lastQueryPostRequest : QueryPostRequest? = null

    var lastEventData : QueryResponseEvent? = null

    fun createNewChatSession() {
        _updateCurrentSessionId.value = false
        updateCurrentSessionId(Utils.getNewSessionId())
    }

    fun getLastSessionId() {
        viewModelScope.launch {
            chatRepository.getLastSessionId().collect {
                if(updateCurrentSessionId.value) {
                    _currentSessionId.value = it
                }
            }
        }
    }

    fun searchMessages(query : String) : Flow<List<MessageEntity>> {
        val response = chatRepository.getSearchResult(query)
        return response
    }

    fun getMessagesBySessionId(sessionId: String): Flow<List<MessageEntity>> {
        val response = chatRepository.getMessagesBySessionId(sessionId)
        if(response.data != null) {
            return response.data
        } else {
            return flow{}
        }
    }

    fun getLastMessagesForEachSession() {
        viewModelScope.launch {
            val response = chatRepository.getLastMessagesOfEachSessionId()
            when(response) {
                is Response.Loading -> {
                }
                is Response.Success -> {
                    response.data?.let { lastMessages ->
                        _lastMessagesSession.value = lastMessages
                        if(lastMessages.isNullOrEmpty()) {
                            _updateCurrentSessionId.value = false
                            updateCurrentSessionId(Utils.getNewSessionId())
                        }
                    }
                }
                is Response.Error -> {
                }
            }
        }
    }

    fun updateCurrentSessionId(sessionId: String) {
        viewModelScope.launch {
            _currentSessionId.value = sessionId
        }
    }

    fun queryPost(newMsgId : Int,queryPostRequest: QueryPostRequest) {
        viewModelScope.launch {
            lastQueryPostRequest = queryPostRequest
            insertQueryInLocalDatabase(newMsgId, queryPostRequest)
            _updateCurrentSessionId.value = true
            chatRepository.queryPost(queryPostRequest = queryPostRequest).collect {
                Log.d("ChatViewModel","Event Collected! ${it}")
                val eventData : QueryResponseEvent = Gson().fromJson(it,QueryResponseEvent::class.java)
                handleEventData(eventData)
            }
        }
    }

    private fun insertQueryInLocalDatabase(newMsgId: Int,queryPostRequest: QueryPostRequest) {
        var lastMsgId = newMsgId
        queryPostRequest.body.messages?.let {
            if(it.isNotEmpty()) {
                it.forEach { queryMessage->
                    queryMessage?.let { currentMsg ->
                        insertMessage(
                            message = MessageEntity(
                                msgId = lastMsgId,
                                sessionId = currentSessionId.value,
                                createdAt = Utils.getCurrentUTCEpochMillis(),
                                messageFiles = null,
                                messageText = queryMessage.text,
                                htmlString = null,
                                role = MessageRole.USER
                            )
                        )
                        lastMsgId += 1
                    }
                }
            }
        }
    }

    private fun handleEventData(eventData: QueryResponseEvent) {
        if(eventData.msgId == null || eventData.overwrite == null || eventData.text == null) {
            _lastQueryResponse.value = Response.Error(message = "Something went wrong!")
            return
        }
        if(eventData.overwrite == true) {
            insertMessage(
                message = MessageEntity(
                    msgId = eventData.msgId,
                    sessionId = currentSessionId.value,
                    createdAt = Utils.getCurrentUTCEpochMillis(),
                    messageFiles = null,
                    messageText = eventData.text,
                    htmlString = null,
                    role = MessageRole.AI
                )
            )
        } else {
            lastEventData?.let {
                if(it.msgId == eventData.msgId) {
                    val combinedMessage = it.text + eventData.text
                    insertMessage(
                        message = MessageEntity(
                            msgId = eventData.msgId,
                            sessionId = currentSessionId.value,
                            createdAt = Utils.getCurrentUTCEpochMillis(),
                            messageFiles = null,
                            messageText = combinedMessage,
                            htmlString = null,
                            role = MessageRole.AI
                        )
                    )
                }
            }
        }
        lastEventData = eventData
    }

    fun insertMessage(message: MessageEntity) {
        viewModelScope.launch {
            chatRepository.insertMessages(listOf(message))
        }
    }

    fun insertMessage(messageList : List<MessageEntity>) {
        viewModelScope.launch {
            chatRepository.insertMessages(messageList)
        }
    }
}