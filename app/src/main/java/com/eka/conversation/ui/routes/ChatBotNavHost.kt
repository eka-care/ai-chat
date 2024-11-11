package com.eka.conversation.ui.routes

import androidx.activity.OnBackPressedDispatcher
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.eka.conversation.common.models.ChatInitConfiguration
import com.eka.conversation.ui.presentation.models.BottomSectionConfiguration
import com.eka.conversation.ui.presentation.models.ContentSectionConfiguration
import com.eka.conversation.ui.presentation.models.ThreadScreenConfiguration
import com.eka.conversation.ui.presentation.models.TopBarConfiguration
import com.eka.conversation.ui.presentation.screens.ChatScreen
import com.eka.conversation.ui.presentation.screens.ThreadScreen
import com.eka.conversation.ui.presentation.viewmodels.ChatViewModel

@Composable
fun ChatBotNavHost(
    navController: NavHostController,
    chatViewModel : ChatViewModel,
    chatInitConfiguration: ChatInitConfiguration,
    onBackPressedDispatcher : OnBackPressedDispatcher
) {
    NavHost(navController = navController, startDestination = Screen.Chat.route) {
        composable(Screen.Chat.route) {
            ChatScreen(
                viewModel = chatViewModel,
                topBarConfiguration = chatInitConfiguration.topBarConfiguration?.copy(
                    onTrailingIconClick = {
                        navController.navigate(Screen.Threads.route)
                    },
                    onLeadingIconClick = {
                        chatInitConfiguration.topBarConfiguration.onLeadingIconClick.invoke()
                        onBackPressedDispatcher.onBackPressed()
                    },
                ) ?: TopBarConfiguration.defaults(
                    onLeadingIconClick = {
                         onBackPressedDispatcher.onBackPressed()
                    },
                    onTrailingIconClick = {
                        navController.navigate(Screen.Threads.route)
                    }
                ),
                bottomSectionConfiguration = chatInitConfiguration.bottomSectionConfiguration ?: BottomSectionConfiguration.defaults(),
                contentSectionConfiguration = chatInitConfiguration.contentSectionConfiguration ?: ContentSectionConfiguration.defaults(),
            )
        }
        composable(Screen.Threads.route) {
            ThreadScreen(
                goBackToChatScreen = {
                    onBackPressedDispatcher.onBackPressed()
                },
                threadScreenConfiguration = chatInitConfiguration.threadScreenConfiguration ?: ThreadScreenConfiguration.defaults(),
                viewModel = chatViewModel
            )
        }
    }
}