package com.eka.conversation.ui.presentation.screens

import androidx.activity.OnBackPressedDispatcher
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.eka.conversation.ChatInit
import com.eka.conversation.ui.presentation.viewmodels.ChatViewModel
import com.eka.conversation.ui.routes.ChatBotNavHost
import com.eka.conversation.ui.theme.ChatBotTheme

@Composable
fun ConversationScreen(
    viewModel: ChatViewModel,
    onBackPressedDispatcher : OnBackPressedDispatcher
){
    val navController =  rememberNavController()

    val chatInitConfiguration = ChatInit.getChatInitConfiguration()

    LaunchedEffect(Unit) {
        viewModel.initNewChatSession()
        viewModel.getLastMessagesForEachSession()
    }

    ChatBotTheme() {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            ChatBotNavHost(
                navController = navController,
                chatViewModel = viewModel,
                onBackPressedDispatcher = onBackPressedDispatcher,
                chatInitConfiguration = chatInitConfiguration
            )
        }
    }
}