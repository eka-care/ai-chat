package com.eka.conversation.ui.presentation.models

import androidx.annotation.Keep
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Keep
data class BottomSectionConfiguration(
    val modifier: Modifier = Modifier,
    val leadingIcon: @Composable (() -> Unit)? = null,
    val onLeadingIconClick: () -> Unit = {},
    val trailingIcon: @Composable  (() -> Unit)? = null,
    val onTrailingIconClick: () -> Unit = {},
    val chatInputAreaConfiguration : ChatInputAreaConfiguration = ChatInputAreaConfiguration.defaults()
) {
    companion object {
        fun defaults(
            modifier: Modifier = Modifier,
            leadingIcon: @Composable (() -> Unit)? = null,
            onLeadingIconClick: () -> Unit = {},
            trailingIcon: @Composable (() -> Unit)? = null,
            onTrailingIconClick: () -> Unit = {},
            chatInputAreaConfiguration: ChatInputAreaConfiguration = ChatInputAreaConfiguration.defaults()
        ) : BottomSectionConfiguration {
            return BottomSectionConfiguration(
                modifier = modifier,
                chatInputAreaConfiguration = chatInputAreaConfiguration,
                leadingIcon = leadingIcon,
                onLeadingIconClick = onLeadingIconClick,
                trailingIcon = trailingIcon ?: {
                    Icon(
                        modifier = Modifier
                            .size(32.dp),
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color.Black
                    )
                },
                onTrailingIconClick = onTrailingIconClick
            )
        }
    }
}
