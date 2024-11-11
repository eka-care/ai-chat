package com.eka.conversation.ui.routes

sealed class Screen(val route: String) {
    object Chat : Screen("chat")
    object Threads : Screen("threads")
}