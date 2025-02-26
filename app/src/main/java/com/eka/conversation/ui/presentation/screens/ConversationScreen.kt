package com.eka.conversation.ui.presentation.screens

import androidx.activity.OnBackPressedDispatcher
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.eka.conversation.ChatInit
import com.eka.conversation.data.remote.api.RetrofitClient
import com.eka.conversation.ui.presentation.viewmodels.ChatViewModel
import com.eka.conversation.ui.routes.ChatBotNavHost
import com.eka.conversation.ui.routes.Screen
import com.eka.conversation.ui.theme.ChatBotTheme

@Composable
fun ConversationScreen(
    viewModel: ChatViewModel,
    onBackPressedDispatcher : OnBackPressedDispatcher
){
    val navController =  rememberNavController()

    val chatInitConfiguration = ChatInit.getChatInitConfiguration()
    var initialScreen: Screen = Screen.Threads
    if (chatInitConfiguration.chatGeneralConfiguration.isChatFirstScreen) {
        initialScreen = Screen.Chat
    }

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