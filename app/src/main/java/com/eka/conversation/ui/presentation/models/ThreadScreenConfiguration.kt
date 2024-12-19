package com.eka.conversation.ui.presentation.models

import androidx.annotation.Keep
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    val threadScreenTopBarConfiguration: ThreadsTopBarConfiguration = ThreadsTopBarConfiguration.defaults(),
    val newChatButton: @Composable ((onNewChatClick: () -> Unit) -> Unit)? = null,
    val threadItem : @Composable ((MessageContent) -> Unit)? = null,
    val searchItem: @Composable ((MessageContent) -> Unit)? = null,
    val searchBarConfiguration : SearchBarConfiguration = SearchBarConfiguration()
) {
    companion object {
        fun defaults(
            modifier: Modifier = Modifier,
            threadScreenTopBarConfiguration: ThreadsTopBarConfiguration = ThreadsTopBarConfiguration.defaults(),
            newChatButton: @Composable ((onNewChatClick: () -> Unit) -> Unit)? = null,
            threadItem : @Composable ((MessageContent) -> Unit)? = null,
            searchItem: @Composable ((MessageContent) -> Unit)? = null,
            searchBarConfiguration : SearchBarConfiguration = SearchBarConfiguration()
        ) : ThreadScreenConfiguration {
            return ThreadScreenConfiguration(
                modifier = modifier,
                threadScreenTopBarConfiguration = threadScreenTopBarConfiguration,
                newChatButton = newChatButton ?: { onNewChatClick ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color = Gray100)
                            .padding(16.dp)
                    ) {
                        NewChatButton(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            onClick = { }
                        )
                        Box(modifier = Modifier
                            .matchParentSize()
                            .clickable {
                                onNewChatClick.invoke()
                            }) {}
                    }
                },
                threadItem = threadItem ?: { messageContent ->
                    ThreadSessionItem(messageContent = messageContent) {

                    }
                },
                searchItem = searchItem ?: { messageContent ->
                    ThreadSessionItem(messageContent = messageContent) {

                    }
                },
                searchBarConfiguration = searchBarConfiguration
            )
        }
    }
}
