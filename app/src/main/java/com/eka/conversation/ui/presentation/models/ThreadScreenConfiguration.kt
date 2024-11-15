package com.eka.conversation.ui.presentation.models

import androidx.annotation.Keep
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eka.conversation.data.local.db.entities.models.MessageContent
import com.eka.conversation.ui.presentation.components.ThreadSessionItem
import com.eka.conversation.ui.presentation.screens.NewChatButton
import com.eka.conversation.ui.theme.Gray100

@Keep
data class ThreadScreenConfiguration(
    val modifier: Modifier = Modifier,
    val topBarConfiguration: TopBarConfiguration? = null,
    val newChatButton : @Composable (() -> Unit)? = null,
    val onNewChatClick : () -> Unit,
    val threadItem : @Composable ((MessageContent) -> Unit)? = null,
    val searchBarConfiguration : SearchBarConfiguration = SearchBarConfiguration()
) {
    companion object {
        fun defaults(
            modifier: Modifier = Modifier,
            topBarConfiguration: TopBarConfiguration? = null,
            newChatButton : @Composable (() -> Unit)? = null,
            onNewChatClick : (() -> Unit)? = null,
            threadItem : @Composable ((MessageContent) -> Unit)? = null,
            searchBarConfiguration : SearchBarConfiguration = SearchBarConfiguration()
        ) : ThreadScreenConfiguration {
            return ThreadScreenConfiguration(
                modifier = modifier,
                topBarConfiguration = topBarConfiguration,
                onNewChatClick = onNewChatClick ?: {},
                newChatButton = newChatButton ?: {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color = Gray100)
                            .padding(16.dp)
                    ) {
                        NewChatButton(
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                },
                threadItem = threadItem ?: { messageContent ->
                    ThreadSessionItem(messageContent = messageContent) {

                    }
                },
                searchBarConfiguration = searchBarConfiguration
            )
        }
    }
}
