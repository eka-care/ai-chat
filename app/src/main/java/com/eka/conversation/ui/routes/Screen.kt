package com.eka.conversation.ui.routes

sealed class Screen(val route: String) {
    object Chat : Screen("chat?sessionId={sessionId}") {
        fun createRoute(sessionId: String? = null): String {
            return if (sessionId != null) {
                "chat?sessionId=$sessionId"
            } else {
                "chat"
            }
        }
    }
    object Threads : Screen("threads")
}