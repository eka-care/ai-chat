package com.eka.conversation.ui.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.eka.conversation.ChatInit
import com.eka.conversation.common.Utils
import com.eka.conversation.data.local.db.entities.MessageEntity
import com.eka.conversation.ui.presentation.components.SearchBar
import com.eka.conversation.ui.presentation.components.ThreadSessionItem
import com.eka.conversation.ui.presentation.models.ThreadScreenConfiguration
import com.eka.conversation.ui.presentation.viewmodels.ChatViewModel

@Composable
fun SearchScreen(
    onBackPressed: () -> Unit,
    onSearchItemClick: (String) -> Unit,
    threadScreenConfiguration: ThreadScreenConfiguration = ThreadScreenConfiguration.defaults(),
    viewModel: ChatViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(emptyList<MessageEntity>()) }
    val listState = rememberLazyListState()

    LaunchedEffect(searchQuery) {
        viewModel.searchMessages(searchQuery).collect { results ->
            searchResults = results
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0x0F000000))
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Search bar
        SearchBar(
            modifier = Modifier
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                ),
            query = searchQuery,
            onQueryChanged = { searchQuery = it },
            onClear = { searchQuery = "" },
            onBack = onBackPressed
        )

        Spacer(modifier = Modifier
            .height(1.dp)
            .background(color = Color(0xFF79747E)))

        LazyColumn(
            modifier = Modifier
                .defaultMinSize(minHeight = 100.dp)
                .fillMaxWidth()
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                ),
            state = listState,
        ) {
            items(searchResults) { message ->
                SearchItem(
                    message = message,
                    threadScreenConfiguration = threadScreenConfiguration
                ) {
                    onSearchItemClick(message.sessionId)
                }
            }
        }
    }
}

@Composable
fun SearchItem(
    message: MessageEntity,
    threadScreenConfiguration: ThreadScreenConfiguration,
    onItemClick: () -> Unit
) {
    Box {
        if (threadScreenConfiguration.searchItem == null) {
            ThreadSessionItem(
                messageContent = Utils.convertToMessageContent(
                    messageEntity = message
                )
            ) {

            }
        } else {
            threadScreenConfiguration.searchItem.invoke(
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