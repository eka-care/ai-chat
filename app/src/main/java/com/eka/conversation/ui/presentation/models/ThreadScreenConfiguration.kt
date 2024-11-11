package com.eka.conversation.ui.presentation.models

import androidx.annotation.Keep
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eka.conversation.data.local.db.entities.models.MessageContent
import com.eka.conversation.ui.presentation.components.ChatScreenTopBar
import com.eka.conversation.ui.presentation.components.ThreadSessionItem
import com.eka.conversation.ui.presentation.screens.NewChatButton
import com.eka.conversation.ui.theme.styleTitlesHeadLine

@Keep
data class ThreadScreenConfiguration(
    val modifier: Modifier = Modifier,
    val topBar : @Composable (() -> Unit)? = null,
    val newChatButton : @Composable (() -> Unit)? = null,
    val onNewChatClick : () -> Unit,
    val threadItem : @Composable ((MessageContent) -> Unit)? = null,
    val searchBarConfiguration : SearchBarConfiguration = SearchBarConfiguration()
) {
    companion object {
        fun defaults(
            modifier: Modifier = Modifier,
            topBar : @Composable (() -> Unit)? = null,
            newChatButton : @Composable (() -> Unit)? = null,
            onNewChatClick : (() -> Unit)? = null,
            threadItem : @Composable ((MessageContent) -> Unit)? = null,
            searchBarConfiguration : SearchBarConfiguration = SearchBarConfiguration()
        ) : ThreadScreenConfiguration {
            return ThreadScreenConfiguration(
                modifier = modifier,
                topBar = topBar,
                onNewChatClick = onNewChatClick ?: {},
                newChatButton = newChatButton ?: {
                    NewChatButton(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
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
