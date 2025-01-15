package com.eka.conversation.ui.presentation.screens

import android.app.Application
import android.content.Context
import androidx.activity.OnBackPressedDispatcher
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.eka.conversation.ChatInit
import com.eka.conversation.data.remote.api.RetrofitClient
import com.eka.conversation.ui.presentation.viewmodels.ChatViewModel
import com.eka.conversation.ui.presentation.viewmodels.factories.ChatViewModelFactory
import com.eka.conversation.ui.routes.ChatBotNavHost
import com.eka.conversation.ui.routes.Screen
import com.eka.conversation.ui.theme.ChatBotTheme

@Composable
fun ConversationScreen(
    context: Context,
//    viewModel: ChatViewModel,
    onBackPressedDispatcher : OnBackPressedDispatcher
){
    val navController =  rememberNavController()

    val viewModel: ChatViewModel = viewModel(
        factory = ChatViewModelFactory(context.applicationContext as Application)
    )

    val chatInitConfiguration = ChatInit.getChatInitConfiguration()
    var initialScreen: Screen = Screen.Threads
    if (chatInitConfiguration.chatGeneralConfiguration.isChatFirstScreen) {
        initialScreen = Screen.Chat
    }

    ChatInit.setChatViewModel(viewModel)

    LaunchedEffect(Unit) {
        RetrofitClient.init(
            baseUrl = chatInitConfiguration.networkConfiguration.baseUrl
        )
        if (chatInitConfiguration.chatGeneralConfiguration.filterApplyOnOwnerId) {
            viewModel.getLastMessagesForEachSession(ownerId = chatInitConfiguration.chatGeneralConfiguration.ownerId)
        } else {
            viewModel.getLastMessagesForEachSession(ownerId = "")
        }
    }

    ChatBotTheme() {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            ChatBotNavHost(
                navController = navController,
                chatViewModel = viewModel,
                onBackPressedDispatcher = onBackPressedDispatcher,
                chatInitConfiguration = chatInitConfiguration,
                initialScreen = initialScreen
            )
        }
    }
}