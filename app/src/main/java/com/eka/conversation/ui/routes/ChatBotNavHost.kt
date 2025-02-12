package com.eka.conversation.ui.routes

import androidx.activity.OnBackPressedDispatcher
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.eka.conversation.common.Utils
import com.eka.conversation.common.models.ChatInitConfiguration
import com.eka.conversation.ui.presentation.models.BottomSectionConfiguration
import com.eka.conversation.ui.presentation.models.ContentSectionConfiguration
import com.eka.conversation.ui.presentation.models.ThreadScreenConfiguration
import com.eka.conversation.ui.presentation.models.TopBarConfiguration
import com.eka.conversation.ui.presentation.screens.ChatScreen
import com.eka.conversation.ui.presentation.screens.SearchScreen
import com.eka.conversation.ui.presentation.screens.ThreadScreen
import com.eka.conversation.ui.presentation.viewmodels.ChatViewModel

@Composable
fun ChatBotNavHost(
    navController: NavHostController,
    chatViewModel: ChatViewModel,
    chatInitConfiguration: ChatInitConfiguration,
    onBackPressedDispatcher: OnBackPressedDispatcher,
    initialScreen: Screen = Screen.Chat
) {
    NavHost(navController = navController, startDestination = initialScreen.route) {
        // Chat Screen
        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("sessionId") {
                type = NavType.StringType; nullable = true
            })
        ) { backStackEntry ->
            var sessionId = backStackEntry.arguments?.getString("sessionId")
            var isFromThreadScreen = !sessionId.isNullOrEmpty()
            if (sessionId.isNullOrEmpty()) {
                sessionId = Utils.getNewSessionId()
            }
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
                    },
                    titleName = "General Chat",
                    subTitleName = "Ask Anything!"
                ),
                bottomSectionConfiguration = chatInitConfiguration.bottomSectionConfiguration
                    ?: BottomSectionConfiguration.defaults(),
                contentSectionConfiguration = chatInitConfiguration.contentSectionConfiguration
                    ?: ContentSectionConfiguration.defaults(),
                sessionId = sessionId,
                isFromThreadScreen = isFromThreadScreen
            )
        }

        // Threads Screen
        composable(Screen.Threads.route) {
            ThreadScreen(
                goBackToChatScreen = {
                    onBackPressedDispatcher.onBackPressed()
                },
                onSearchClick = {
                    navController.navigate(Screen.Search.route)
                },
                goToChatScreen = { sessionId ->
                    navController.navigate(Screen.Chat.createRoute(sessionId))
                },
                threadScreenConfiguration = chatInitConfiguration.threadScreenConfiguration
                    ?: ThreadScreenConfiguration.defaults(),
                viewModel = chatViewModel
            )
        }

        // Search Screen
        composable(Screen.Search.route) {
            SearchScreen(
                onBackPressed = {
                    onBackPressedDispatcher.onBackPressed()
                },
                onSearchItemClick = { sessionId ->
                    navController.navigate(Screen.Chat.createRoute(sessionId)) {
                        popUpTo(Screen.Search.route) { inclusive = true }
                    }
                },
                threadScreenConfiguration = chatInitConfiguration.threadScreenConfiguration
                    ?: ThreadScreenConfiguration.defaults(),
                viewModel = chatViewModel
            )
        }
    }
}
