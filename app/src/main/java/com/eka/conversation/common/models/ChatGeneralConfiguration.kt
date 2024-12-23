package com.eka.conversation.common.models

import androidx.compose.runtime.Composable
import com.eka.conversation.data.local.db.entities.MessageEntity

data class ChatGeneralConfiguration(
    val isChatFirstScreen: Boolean = true,
    val shouldShowThreadsIconOnChatScreen: Boolean = true,
    val chatContext: String,
    val chatSubContext: String,
    val chatSessionConfig: String,
    val onSessionInvokeNetworkConfiguration: (String) -> NetworkConfiguration,
    val isSortEnabled: Boolean = true,
    val isSearchEnabled: Boolean = true,
    val sortBottomSheetLayout: @Composable (() -> Unit) -> Unit?,
    val onSortItemClick: (List<MessageEntity>) -> List<MessageEntity>,
    val sessionIdentity: String?,
    val shouldUseExistingSession: Boolean,
)