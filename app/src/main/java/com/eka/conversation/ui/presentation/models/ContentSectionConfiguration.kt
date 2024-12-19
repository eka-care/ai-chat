package com.eka.conversation.ui.presentation.models

import androidx.annotation.Keep
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.eka.conversation.data.local.db.entities.models.MessageContent
import com.eka.conversation.ui.presentation.components.ErrorMessageContainer
import com.eka.conversation.ui.presentation.components.ReceiveMessageContainer
import com.eka.conversation.ui.presentation.components.SendMessageContainer

@Keep
data class ContentSectionConfiguration(
    val modifier: Modifier = Modifier,
    val sendMessageContainer : @Composable (MessageContent) -> Unit = {},
    val receiveMessageContainer : @Composable (MessageContent) -> Unit = {},
    val errorMessageContainer : @Composable (MessageContent) -> Unit = {},
    val loadingMessageContainer : @Composable () -> Unit = {},
    val background: @Composable () -> Unit = {},
    val newChatBackground: @Composable () -> Unit = {},
    val reverseMessageSides : Boolean = false
) {
    companion object {
        fun defaults(
            modifier: Modifier = Modifier,
            sendMessageContainer : @Composable ((MessageContent) -> Unit)? = null,
            receiveMessageContainer : @Composable ((MessageContent) -> Unit)? = null,
            errorMessageContainer : @Composable ((MessageContent) -> Unit)? = null,
            loadingMessageContainer : @Composable (() -> Unit)? = null,
            background: @Composable () -> Unit = {},
            newChatBackground: @Composable () -> Unit = {},
            reverseMessageSides : Boolean = false
        ) : ContentSectionConfiguration {

            return ContentSectionConfiguration(
                modifier = modifier,
                background = background,
                newChatBackground = newChatBackground,
                reverseMessageSides = reverseMessageSides,
                sendMessageContainer = sendMessageContainer ?: { messageContent ->
                    SendMessageContainer(messageContent)
                },
                receiveMessageContainer = receiveMessageContainer ?: { messageContent ->
                    ReceiveMessageContainer(messageContent)
                },
                errorMessageContainer = errorMessageContainer ?: { messageContent ->
                    ErrorMessageContainer(messageContent)
                },
                loadingMessageContainer = loadingMessageContainer ?: {
                    CircularProgressIndicator()
                }
            )
        }
    }
}
