package com.eka.conversation

import android.content.Context
import com.eka.conversation.common.models.ChatInitConfiguration
import com.eka.conversation.data.remote.api.RetrofitClient
import com.eka.conversation.ui.presentation.viewmodels.ChatViewModel

object ChatInit {
    private var configuration : ChatInitConfiguration? = null
    private var viewModel: ChatViewModel? = null

    fun initialize(
        chatInitConfiguration : ChatInitConfiguration,
        context : Context
    ) {
        configuration = chatInitConfiguration
        RetrofitClient.init(
            baseUrl = chatInitConfiguration.networkConfiguration.baseUrl
        )
//        context.startActivity(Intent(context,ConversationActivity::class.java))
    }

    fun getChatInitConfiguration() : ChatInitConfiguration {
        if(configuration == null) {
            throw IllegalStateException("Chat Init configuration not initialized")
        }
        return configuration!!
    }

    fun setChatViewModel(chatViewModel: ChatViewModel) {
        viewModel = chatViewModel
    }

    fun getChatViewModel(): ChatViewModel? {
        return viewModel
    }

    fun changeConfiguration(
        chatInitConfiguration: ChatInitConfiguration,
    ) {
        configuration = chatInitConfiguration
    }
}