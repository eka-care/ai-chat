package com.eka.conversation.ui.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eka.conversation.common.Utils
import com.eka.conversation.data.local.db.entities.MessageEntity
import com.eka.conversation.data.local.db.entities.models.MessageRole
import com.eka.conversation.ui.presentation.models.ContentSectionConfiguration
import com.eka.conversation.ui.presentation.viewmodels.ChatViewModel

@Composable
fun ChatScreenContentSection(
    modifier: Modifier = Modifier,
    contentSectionConfiguration: ContentSectionConfiguration = ContentSectionConfiguration.defaults(),
    viewModel: ChatViewModel,
    sessionId: String
) {
    val listState = rememberLazyListState()
    var messages by remember { mutableStateOf<List<MessageEntity>>(emptyList()) }
    val uiState by viewModel.chatScreenUiState.collectAsState()

    LaunchedEffect(sessionId) {
        viewModel.getMessagesBySessionId(sessionId).collect { newMessages ->
            messages = newMessages.reversed()
        }
    }

    LaunchedEffect(messages.size) {
        if(messages.size > 0) {
            listState.animateScrollToItem(0)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        if (messages.isNullOrEmpty()) {
            contentSectionConfiguration.newChatBackground.invoke()
        } else {
            contentSectionConfiguration.background.invoke()
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            reverseLayout = true,
            state = listState,
        ) {
            items(items = messages, key = {it.msgId} ) { message ->
                when(message.role) {
                    MessageRole.USER -> {
                        contentSectionConfiguration.sendMessageContainer(
                            Utils.convertToMessageContent(messageEntity = message)
                        )
                    }
                    MessageRole.AI -> {
                        contentSectionConfiguration.receiveMessageContainer(
                            Utils.convertToMessageContent(messageEntity = message)
                        )
                    }
                    MessageRole.CUSTOM -> {

                    }
                    else -> {
                    }
                }
            }
        }
    }
}