package com.eka.conversation.ui.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eka.conversation.common.Utils
import com.eka.conversation.data.local.db.entities.MessageEntity
import com.eka.conversation.ui.presentation.components.ThreadScreenTopBar
import com.eka.conversation.ui.presentation.components.ThreadSessionItem
import com.eka.conversation.ui.presentation.models.ThreadScreenConfiguration
import com.eka.conversation.ui.presentation.models.ThreadsTopBarConfiguration
import com.eka.conversation.ui.presentation.viewmodels.ChatViewModel
import com.eka.conversation.ui.theme.Gray700
import com.eka.conversation.ui.theme.styleTitlesHeadLine

@Composable
fun ThreadScreen(
    goBackToChatScreen: () -> Unit,
    goToChatScreen: (String) -> Unit,
    onSearchClick: () -> Unit,
    viewModel: ChatViewModel,
    threadScreenConfiguration: ThreadScreenConfiguration = ThreadScreenConfiguration.defaults()
) {
    val listState = rememberLazyListState()
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(emptyList<MessageEntity>()) }
    val lastMessagesForEachSession by viewModel.lastMessagesSession.collectAsState()

    LaunchedEffect(searchQuery) {
        if(searchQuery.isEmpty()) {
            viewModel.getLastMessagesForEachSession()
        } else {
            viewModel.searchMessages(searchQuery).collect { results ->
                searchResults = results
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        if (threadScreenConfiguration.threadScreenTopBarConfiguration == null) {
            ThreadScreenTopBar(
                threadsTopBarConfiguration = ThreadsTopBarConfiguration.defaults(),
                onBackClick = goBackToChatScreen,
                onSearchClick = onSearchClick,
                onSortClick = {
                    viewModel.onSortClick()
                }
            )
        } else {
            ThreadScreenTopBar(
                threadsTopBarConfiguration = threadScreenConfiguration.threadScreenTopBarConfiguration,
                onBackClick = goBackToChatScreen,
                onSearchClick = onSearchClick,
                onSortClick = {
                    viewModel.onSortClick()
                }
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
        ) {
            if(searchQuery.isEmpty()) {
                lastMessagesForEachSession?.let { lastMessages ->
                    items(lastMessages) { message ->
                        ThreadItem(
                            message = message,
                            threadScreenConfiguration = threadScreenConfiguration
                        ) {
                            goToChatScreen(message.sessionId)
                        }
                    }
                }
            } else {
                items(searchResults) { message ->
                    ThreadItem(
                        message = message,
                        threadScreenConfiguration = threadScreenConfiguration
                    ) {
                        goToChatScreen(message.sessionId)
                    }
                }
            }
        }

        if (threadScreenConfiguration.newChatButton == null) {
            NewChatButton(
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = {
                    goToChatScreen(Utils.getNewSessionId())
                }
            )
        } else {
            threadScreenConfiguration.newChatButton.invoke {
                goToChatScreen(Utils.getNewSessionId())
            }
        }
    }
}

@Composable
fun ThreadItem(
    message: MessageEntity,
    threadScreenConfiguration : ThreadScreenConfiguration,
    onItemClick : () -> Unit
) {
    Box{
        if(threadScreenConfiguration.threadItem == null) {
            ThreadSessionItem(
                messageContent = Utils.convertToMessageContent(
                    messageEntity = message
                )
            ) {

            }
        } else {
            threadScreenConfiguration.threadItem.invoke(
                Utils.convertToMessageContent(messageEntity = message)
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable {
                    onItemClick.invoke()
                }
        )
    }
}

@Composable
fun NewChatButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(color = Gray700, shape = RoundedCornerShape(corner = CornerSize(8.dp)))
            .fillMaxWidth()
            .clickable {
                onClick.invoke()
            }
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.Center),
            text = "Start a new Chat",
            color = Color.White,
            style = styleTitlesHeadLine,
        )
    }
}
