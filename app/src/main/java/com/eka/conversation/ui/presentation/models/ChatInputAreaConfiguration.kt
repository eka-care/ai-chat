package com.eka.conversation.ui.presentation.models

import androidx.annotation.Keep
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Keep
data class ChatInputAreaConfiguration(
    val modifier: Modifier = Modifier,
    val hint : @Composable (() -> Unit)? = null,
    val leadingIcon: @Composable (() -> Unit)? = null,
    val trailingIcon: @Composable (() -> Unit)? = null,
) {
    companion object {
        fun defaults(
             modifier: Modifier = Modifier,
             hint : @Composable (() -> Unit)? = null,
             trailingIcon: @Composable (() -> Unit)? = null,
             leadingIcon: @Composable (() -> Unit)? = null,
        ) : ChatInputAreaConfiguration {
            return ChatInputAreaConfiguration(
                modifier = modifier,
                hint = hint ?: {
                    Text(
                        text = "Ask your queries",
                    )
                },
                trailingIcon = trailingIcon,
                leadingIcon = leadingIcon,
            )
        }
    }
}
