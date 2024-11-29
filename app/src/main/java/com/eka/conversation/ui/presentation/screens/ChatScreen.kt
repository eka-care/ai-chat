package com.eka.conversation.ui.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.eka.conversation.ChatInit
import com.eka.conversation.common.Utils
import com.eka.conversation.data.local.db.entities.MessageEntity
import com.eka.conversation.data.local.db.entities.models.MessageRole
import com.eka.conversation.data.remote.models.PostMessage
import com.eka.conversation.data.remote.models.QueryPostBody
import com.eka.conversation.data.remote.models.QueryPostRequest
import com.eka.conversation.ui.presentation.components.ChatScreenBottomSection
import com.eka.conversation.ui.presentation.components.ChatScreenContentSection
import com.eka.conversation.ui.presentation.components.ChatScreenTopBar
import com.eka.conversation.ui.presentation.models.BottomSectionConfiguration
import com.eka.conversation.ui.presentation.models.ContentSectionConfiguration
import com.eka.conversation.ui.presentation.models.TopBarConfiguration
import com.eka.conversation.ui.presentation.viewmodels.ChatViewModel

@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    topBarConfiguration: TopBarConfiguration = TopBarConfiguration.defaults(),
    bottomSectionConfiguration: BottomSectionConfiguration = BottomSectionConfiguration.defaults(),
    contentSectionConfiguration: ContentSectionConfiguration = ContentSectionConfiguration.defaults(),
    viewModel: ChatViewModel,
    sessionId: String
) {
    val context = LocalContext.current.applicationContext
    val lastMessagesForEachSession by viewModel.lastMessagesSession.collectAsState(initial = null)
    var currentSessionId = sessionId
    var messages by remember { mutableStateOf<List<MessageEntity>>(emptyList()) }
    val enterButtonEnableState by viewModel.enterButtonEnableState.collectAsState()
    var textInputState by remember {
        mutableStateOf("")
    }
    val chatInitConfiguration = ChatInit.getChatInitConfiguration()

    LaunchedEffect(currentSessionId) {
        viewModel.getMessagesBySessionId(currentSessionId).collect { newMessages ->
            messages = newMessages
        }
    }

    Column(
        modifier = modifier
            .background(Color.White)
            .fillMaxSize()
    ) {
        ChatScreenTopBar(
            modifier = topBarConfiguration.modifier,
            topBarConfiguration = topBarConfiguration
        )
        ChatScreenContentSection(
            modifier = contentSectionConfiguration.modifier.weight(1f),
            contentSectionConfiguration = contentSectionConfiguration,
            viewModel = viewModel,
            sessionId = currentSessionId
        )
        ChatScreenBottomSection(
            modifier = bottomSectionConfiguration.modifier,
            onInputChange = { newValue ->
                 textInputState = newValue
            },
            bottomSectionConfiguration = bottomSectionConfiguration.copy(
                onTrailingIconClick = {
                    if (enterButtonEnableState) {
                        if (Utils.isNetworkAvailable(context = context)) {
                            if (textInputState.trim().isNotEmpty()) {
                                askNewQuery(
                                    newMsgId = getNewMsgId(messages),
                                    textInput = textInputState,
                                    viewModel = viewModel,
                                    params = chatInitConfiguration.networkConfiguration.params,
                                    sessionId = currentSessionId
                                )
                            }
                        } else {
                            Toast.makeText(context, "No Internet!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "Please wait until previous response is completed!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            ),
            chatInitConfiguration = chatInitConfiguration,
            viewModel = viewModel
        )
    }
}

fun getNewMsgId(messages: List<MessageEntity>): Int {
    if(messages.isNullOrEmpty()) {
        return 0
    }
    return messages.last().msgId + 1
}

fun askNewQuery(
    newMsgId : Int,
    textInput : String,
    viewModel: ChatViewModel,
    params: HashMap<String, String>,
    sessionId: String
) {
    params.put("session_id", sessionId)
    viewModel.queryPost(
        newMsgId = newMsgId,
        QueryPostRequest(
            queryParams = params,
            body = QueryPostBody(
                listOf(
                    PostMessage(
                        role = MessageRole.USER.role,
                        text = textInput
                    )
                )
            )
        ),
        sessionId = sessionId
    )
}