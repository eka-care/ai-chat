package com.eka.conversation

import android.content.Context
import android.content.Intent
import android.util.Log
import com.eka.conversation.common.Response
import com.eka.conversation.common.models.ChatInitConfiguration
import com.eka.conversation.common.models.NetworkConfiguration
import com.eka.conversation.data.local.db.ChatDatabase
import com.eka.conversation.data.local.db.entities.MessageEntity
import com.eka.conversation.data.remote.models.QueryResponseEvent
import com.eka.conversation.data.repositories.ChatRepositoryImpl
import com.eka.conversation.domain.repositories.ChatRepository
import com.eka.conversation.features.audio.AndroidAudioRecorder
import com.eka.conversation.ui.presentation.activities.ConversationActivity
import com.eka.conversation.ui.presentation.viewmodels.ChatViewModel
import kotlinx.coroutines.flow.Flow

object ChatInit {
    private var configuration : ChatInitConfiguration? = null
    private var database: ChatDatabase? = null
    private var repository: ChatRepository? = null
    private var viewModel: ChatViewModel? = null

    fun initialize(
        chatInitConfiguration: ChatInitConfiguration?,
        context: Context
    ) {
        configuration = chatInitConfiguration
        database = ChatDatabase.getDatabase(context = context)
        database?.let {
            repository = ChatRepositoryImpl(it)
        }
        Log.d("ChatSDK", "ChatSDK initialized")
    }

    fun getMessagesBySessionId(sessionId: String): Response<Flow<List<MessageEntity>>>? {
        return repository?.getMessagesBySessionId(sessionId)
    }

    fun getSearchResult(query: String, ownerId: String? = null): Flow<List<MessageEntity>>? {
        if (ownerId.isNullOrEmpty()) {
            return repository?.getSearchResult(query = query)
        } else {
            return repository?.getSearchResultWithOwnerId(query = query, ownerId = ownerId)
        }
    }

    fun getAudioRecorder(context: Context): AndroidAudioRecorder {
        return AndroidAudioRecorder(context)
    }

    suspend fun getAllSessions(ownerId: String?): Response<List<MessageEntity>>? {
        return repository?.getAllSession(ownerId = ownerId)
    }

    suspend fun getAllSessionByChatContext(chatContext: String): Response<List<MessageEntity>>? {
        return repository?.getMessagesByContext(chatContext = chatContext)
    }

    suspend fun askNewQuery(
        messageEntity: MessageEntity,
        networkConfiguration: NetworkConfiguration
    ): Flow<QueryResponseEvent>? {
        repository?.insertMessages(listOf(messageEntity))
        Log.d("askNewQuery", messageEntity.toString())
        Log.d("askNewQuery", networkConfiguration.toString())
        val response = repository?.askNewQuery(
            messageEntity = messageEntity,
            networkConfiguration = networkConfiguration
        )
        return response
    }

    suspend fun insertMessages(messages: List<MessageEntity>) {
        repository?.insertMessages(messages)
    }

    fun startChatActivity(context: Context) {
        context.startActivity(Intent(context,ConversationActivity::class.java))
    }

    fun getChatInitConfiguration() : ChatInitConfiguration {
        if(configuration == null) {
            throw IllegalStateException("Chat Init configuration not initialized")
        }
        return configuration!!
    }

    fun changeConfiguration(
        chatInitConfiguration: ChatInitConfiguration,
    ) {
        configuration = chatInitConfiguration
    }
}