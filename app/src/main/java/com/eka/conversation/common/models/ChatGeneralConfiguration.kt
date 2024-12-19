package com.eka.conversation.common.models

data class ChatGeneralConfiguration(
    val isChatFirstScreen: Boolean = true,
    val shouldShowThreadsIconOnChatScreen: Boolean = true,
    val chatContext: String,
    val chatSubContext: String,
    val chatSessionConfig: String,
    val onSessionInvokeNetworkConfiguration: (String) -> NetworkConfiguration
)