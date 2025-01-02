package com.eka.conversation.ui.presentation.screens

import android.util.Log
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
    topBarConfiguration: TopBarConfiguration = TopBarConfiguration.defaults(
        titleName = "General Chat",
        subTitleName = "Ask anything!",
    ),
    bottomSectionConfiguration: BottomSectionConfiguration = BottomSectionConfiguration.defaults(),
    contentSectionConfiguration: ContentSectionConfiguration = ContentSectionConfiguration.defaults(),
    viewModel: ChatViewModel,
    sessionId: String,
    isFromThreadScreen: Boolean
) {
    val context = LocalContext.current.applicationContext
    val lastMessagesForEachSession by viewModel.lastMessagesSession.collectAsState(initial = null)
    var currentSessionId by remember {
        mutableStateOf(sessionId)
    }
    val newQuery by viewModel.newQuery
    var messages by remember { mutableStateOf<List<MessageEntity>>(emptyList()) }
    val enterButtonEnableState by viewModel.enterButtonEnableState.collectAsState()
    var textInputState by remember {
        mutableStateOf("")
    }
    var chatInitConfiguration = ChatInit.getChatInitConfiguration()
    var chatContext by remember { mutableStateOf(chatInitConfiguration.chatGeneralConfiguration.chatContext) }
    var chatSubContext by remember {
        mutableStateOf(chatInitConfiguration.chatGeneralConfiguration.chatSubContext)
    }
    var sessionIdentity = chatInitConfiguration.chatGeneralConfiguration.sessionIdentity

    if (chatInitConfiguration.chatGeneralConfiguration.shouldUseExistingSession && !isFromThreadScreen) {
        LaunchedEffect(Unit) {
            if (!sessionIdentity.isNullOrEmpty()) {
                viewModel.getSessionIdBySessionIdentity(sessionIdentity = sessionIdentity)
            }
        }
        LaunchedEffect(Unit) {
            viewModel.sessionIdBySessionIdentity.collect { oldSessionId ->
                if (oldSessionId != null) {
                    currentSessionId = oldSessionId
                }
            }
        }
    }

    LaunchedEffect(currentSessionId) {
        viewModel.getMessagesBySessionId(currentSessionId).collect { newMessages ->
            messages = newMessages
            if (!messages.isNullOrEmpty()) {
                chatContext = messages.first().chatContext.toString()
                chatSubContext = messages.first().chatSubContext.toString()
                Log.d("ChatSDK", "$chatContext $chatSubContext")
            }
            if (!messages.isNullOrEmpty() && messages.first().chatSessionConfig != null) {
                val newNetworkConfiguration =
                    chatInitConfiguration.chatGeneralConfiguration.onSessionInvokeNetworkConfiguration(
                        messages.first().chatSessionConfig.toString()
                    )
                ChatInit.changeConfiguration(
                    chatInitConfiguration.copy(
                        networkConfiguration = newNetworkConfiguration
                    )
                )
                chatInitConfiguration = ChatInit.getChatInitConfiguration()
            }
        }
    }

    LaunchedEffect(newQuery) {
        if (Utils.isNetworkAvailable(context = context)) {
            if (newQuery.trim().isNotEmpty()) {
                askNewQuery(
                    newMsgId = getNewMsgId(messages),
                    textInput = newQuery,
                    viewModel = viewModel,
                    params = chatInitConfiguration.networkConfiguration.params,
                    sessionId = currentSessionId,
                    chatContext = chatContext,
                    chatSubContext = chatSubContext,
                    chatSessionConfig = chatInitConfiguration.chatGeneralConfiguration.chatSessionConfig,
                    sessionIdentity = chatInitConfiguration.chatGeneralConfiguration.sessionIdentity,
                    ownerId = chatInitConfiguration.chatGeneralConfiguration.ownerId
                )
            }
        } else {
            Toast.makeText(context, "No Internet!", Toast.LENGTH_SHORT).show()
        }
        viewModel.sendNewQuery("")
    }

    Column(
        modifier = modifier
            .background(Color.White)
            .fillMaxSize()
    ) {
        ChatScreenTopBar(
            modifier = topBarConfiguration.modifier,
            topBarConfiguration = topBarConfiguration,
            title = if (messages.isNullOrEmpty()) topBarConfiguration.titleName else messages.first().chatContext.toString(),
            subTitle = if (messages.isNullOrEmpty()) topBarConfiguration.subTitleName else messages.first().chatSubContext.toString(),
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
                                    sessionId = currentSessionId,
                                    chatContext = chatContext,
                                    chatSubContext = chatSubContext,
                                    chatSessionConfig = chatInitConfiguration.chatGeneralConfiguration.chatSessionConfig,
                                    sessionIdentity = chatInitConfiguration.chatGeneralConfiguration.sessionIdentity,
                                    ownerId = chatInitConfiguration.chatGeneralConfiguration.ownerId
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
    sessionId: String,
    chatContext: String,
    chatSubContext: String,
    chatSessionConfig: String,
    sessionIdentity: String?,
    ownerId: String
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
        sessionId = sessionId,
        chatContext = chatContext,
        chatSubContext = chatSubContext,
        chatSessionConfig = chatSessionConfig,
        sessionIdentity = sessionIdentity,
        ownerId = ownerId
    )
}