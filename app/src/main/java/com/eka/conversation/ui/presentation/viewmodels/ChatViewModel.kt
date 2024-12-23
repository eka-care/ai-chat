package com.eka.conversation.ui.presentation.viewmodels

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eka.conversation.ChatInit
import com.eka.conversation.common.Response
import com.eka.conversation.common.Utils
import com.eka.conversation.common.models.AudioProcessorType
import com.eka.conversation.data.local.db.ChatDatabase
import com.eka.conversation.data.local.db.entities.MessageEntity
import com.eka.conversation.data.local.db.entities.models.MessageFileType
import com.eka.conversation.data.local.db.entities.models.MessageRole
import com.eka.conversation.data.remote.models.QueryPostRequest
import com.eka.conversation.data.remote.models.QueryResponseEvent
import com.eka.conversation.data.repositories.ChatRepositoryImpl
import com.eka.conversation.domain.repositories.ChatRepository
import com.eka.conversation.features.audio.AndroidAudioRecorder
import com.eka.conversation.features.audio.DefaultAudioProcessor
import com.eka.conversation.ui.presentation.states.ChatUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.io.File

class ChatViewModel(
    private val app: Application
) : AndroidViewModel(app) {

    var database: ChatDatabase
    var chatRepository: ChatRepository
    var defaultAudioProcessor: DefaultAudioProcessor
    var audioRecorder: AndroidAudioRecorder
    var chatInitConfiguration = ChatInit.getChatInitConfiguration()

    init {
        database = ChatDatabase.getDatabase(app)
        chatRepository = ChatRepositoryImpl(database)
        defaultAudioProcessor = DefaultAudioProcessor(app)
        audioRecorder = AndroidAudioRecorder(app)
    }

    private val _chatScreenUiState = MutableStateFlow<ChatUiState>(ChatUiState.ChatInitLoading)
    val chatScreenUiState = _chatScreenUiState.asStateFlow()

    private val _enterButtonEnableState = MutableStateFlow<Boolean>(true)
    val enterButtonEnableState = _enterButtonEnableState.asStateFlow()

    private val _lastMessagesSession = MutableStateFlow<List<MessageEntity>?>(null)
    val lastMessagesSession = _lastMessagesSession.asStateFlow()

    private val _lastQueryResponse = MutableStateFlow<Response<Boolean>>(Response.Loading())
    val lastQueryResponse = _lastQueryResponse.asStateFlow()

    var lastQueryPostRequest : QueryPostRequest? = null

    var lastEventData : QueryResponseEvent? = null

    var currentAudioFile: File? = null

    var _currentTranscribeData = MutableStateFlow<Response<String>>(Response.Loading())
    val currentTranscribeData = _currentTranscribeData.asStateFlow()

    var _sessionIdBySessionIdentity = MutableStateFlow<String?>(null)
    val sessionIdBySessionIdentity = _sessionIdBySessionIdentity.asStateFlow()

    var textInputState = mutableStateOf("")

    fun updateTextInputState(newValue: String) {
        textInputState.value = newValue
    }

    fun getSessionIdBySessionIdentity(sessionIdentity: String) {
        viewModelScope.launch {
            val response =
                chatRepository.getSessionIdBySessionIdentity(sessionIdentity = sessionIdentity)
            when (response) {
                is Response.Loading -> {
                }

                is Response.Success -> {
                    _sessionIdBySessionIdentity.value = response.data
                }

                is Response.Error -> {
                }

                else -> {}
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
                    }
                }
                is Response.Error -> {
                }
                else ->{}
            }
        }
    }

    fun onSortClick(messages: List<MessageEntity>) {
        _lastMessagesSession.value = messages
    }

    fun queryPost(
        newMsgId: Int,
        queryPostRequest: QueryPostRequest,
        sessionId: String,
        chatContext: String,
        chatSubContext: String,
        chatSessionConfig: String,
        sessionIdentity: String?
    ) {
        viewModelScope.launch {
            _enterButtonEnableState.value = false
            lastQueryPostRequest = queryPostRequest
            insertQueryInLocalDatabase(
                newMsgId,
                queryPostRequest,
                sessionId,
                chatContext,
                chatSubContext,
                chatSessionConfig,
                sessionIdentity
            )
            chatRepository.queryPost(queryPostRequest = queryPostRequest).collect {
                Log.d("ChatViewModel","Event Collected! ${it}")
                handleEventData(
                    eventData = it,
                    sessionId = sessionId,
                    chatContext = chatContext,
                    chatSubContext = chatSubContext,
                    chatSessionConfig = chatSessionConfig,
                    sessionIdentity = sessionIdentity
                )
            }
        }
    }

    private fun insertQueryInLocalDatabase(
        newMsgId: Int,
        queryPostRequest: QueryPostRequest,
        sessionId: String,
        chatContext: String,
        chatSubContext: String,
        chatSessionConfig: String,
        sessionIdentity: String?,
    ) {
        var lastMsgId = newMsgId
        queryPostRequest.body.messages?.let {
            if(it.isNotEmpty()) {
                it.forEach { queryMessage->
                    queryMessage?.let { currentMsg ->
                        insertMessage(
                            message = MessageEntity(
                                msgId = lastMsgId,
                                sessionId = sessionId,
                                createdAt = Utils.getCurrentUTCEpochMillis(),
                                messageFiles = null,
                                messageText = queryMessage.text,
                                htmlString = null,
                                role = MessageRole.USER,
                                chatContext = chatContext,
                                chatSubContext = chatSubContext,
                                chatSessionConfig = chatSessionConfig,
                                sessionIdentity = sessionIdentity
                            )
                        )
                        lastMsgId += 1
                    }
                }
            }
        }
    }

    private fun handleEventData(
        eventData: QueryResponseEvent,
        sessionId: String,
        chatContext: String,
        chatSubContext: String,
        chatSessionConfig: String,
        sessionIdentity: String?,
    ) {
        if (eventData.isLastEvent) {
            _enterButtonEnableState.value = true
            return
        }
        if(eventData.msgId == null || eventData.overwrite == null || eventData.text == null) {
            _lastQueryResponse.value = Response.Error(message = "Something went wrong!")
            return
        }
        if(eventData.overwrite == true) {
            insertMessage(
                message = MessageEntity(
                    msgId = eventData.msgId,
                    sessionId = sessionId,
                    createdAt = Utils.getCurrentUTCEpochMillis(),
                    messageFiles = null,
                    messageText = eventData.text,
                    htmlString = null,
                    role = MessageRole.AI,
                    chatContext = chatContext,
                    chatSubContext = chatSubContext,
                    chatSessionConfig = chatSessionConfig,
                    sessionIdentity = sessionIdentity
                )
            )
        } else {
            lastEventData?.let {
                if(it.msgId == eventData.msgId) {
                    val combinedMessage = it.text + eventData.text
                    insertMessage(
                        message = MessageEntity(
                            msgId = eventData.msgId,
                            sessionId = sessionId,
                            createdAt = Utils.getCurrentUTCEpochMillis(),
                            messageFiles = null,
                            messageText = combinedMessage,
                            htmlString = null,
                            role = MessageRole.AI,
                            chatContext = chatContext,
                            chatSubContext = chatSubContext,
                            chatSessionConfig = chatSessionConfig,
                            sessionIdentity = sessionIdentity
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

    fun clearRecording() {
        _currentTranscribeData.value = Response.Loading()
        currentAudioFile = null
    }

    // Audio Feature
    fun startAudioRecording() {
        when (chatInitConfiguration.audioFeatureConfiguration.audioProcessorType) {
            AudioProcessorType.CUSTOM -> {
                currentAudioFile =
                    File(app.filesDir, "${Utils.getNewFileName(MessageFileType.AUDIO)}.m4a")
                audioRecorder.startRecording(currentAudioFile!!, onError = { error ->
                    _currentTranscribeData.value = Response.Error(message = error)
                })
            }

            AudioProcessorType.VOSK -> {
            }

            AudioProcessorType.GOOGLE_SPEECH_RECOGNIZER -> {
                defaultAudioProcessor.processAudio(audioFile = null) { response ->
                    _currentTranscribeData.value = response
                }
            }

            else -> {
                throw IllegalStateException("Invalid processor type")
            }
        }
    }

    fun stopAudioRecordingInFileIfStarted() {
        audioRecorder.stopRecording()
    }

    fun stopAudioRecording() {
        when (chatInitConfiguration.audioFeatureConfiguration.audioProcessorType) {
            AudioProcessorType.CUSTOM -> {
                audioRecorder.stopRecording()
                chatInitConfiguration.audioFeatureConfiguration.audioProcessor?.let {
                    it.processAudio(currentAudioFile!!) { response ->
                        _currentTranscribeData.value = response
                    }
                }
            }

            AudioProcessorType.VOSK -> {
            }

            AudioProcessorType.GOOGLE_SPEECH_RECOGNIZER -> {
                // File is not needed for Google Speech Recognizer
                defaultAudioProcessor.stopRecordingAndTranscribing()
            }

            else -> {
                throw IllegalStateException("Invalid processor type")
            }
        }
    }
}