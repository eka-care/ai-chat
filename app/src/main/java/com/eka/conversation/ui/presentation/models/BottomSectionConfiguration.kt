package com.eka.conversation.ui.presentation.models

import androidx.annotation.Keep
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Keep
data class BottomSectionConfiguration(
    val modifier: Modifier = Modifier,
    val leadingIcon: @Composable (() -> Unit)? = null,
    val onLeadingIconClick: () -> Unit = {},
    val trailingIcon: @Composable  (() -> Unit)? = null,
    val onTrailingIconClick: () -> Unit = {},
    val isSubmitIconInsideChatInputArea: Boolean = false,
    val chatInputAreaConfiguration : ChatInputAreaConfiguration = ChatInputAreaConfiguration.defaults()
) {
    companion object {
        fun defaults(
            modifier: Modifier = Modifier,
            leadingIcon: @Composable (() -> Unit)? = null,
            onLeadingIconClick: () -> Unit = {},
            trailingIcon: @Composable (() -> Unit)? = null,
            isSubmitIconInsideChatInputArea: Boolean = false,
            onTrailingIconClick: () -> Unit = {},
            chatInputAreaConfiguration: ChatInputAreaConfiguration = ChatInputAreaConfiguration.defaults()
        ) : BottomSectionConfiguration {
            return BottomSectionConfiguration(
                modifier = modifier,
                chatInputAreaConfiguration = chatInputAreaConfiguration,
                leadingIcon = leadingIcon,
                onLeadingIconClick = onLeadingIconClick,
                trailingIcon = trailingIcon,
                isSubmitIconInsideChatInputArea = isSubmitIconInsideChatInputArea,
                onTrailingIconClick = onTrailingIconClick
            )
        }
    }
}
