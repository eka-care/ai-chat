package com.eka.conversation.ui.presentation.models

import androidx.annotation.Keep
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.eka.conversation.ui.theme.styleBodyFootnote
import com.eka.conversation.ui.theme.styleTitlesSubheadLine

@Keep
data class TopBarConfiguration(
    val modifier: Modifier = Modifier,
    val leadingIcon: @Composable (() -> Unit)? = null,
    val onLeadingIconClick: () -> Unit = {},
    val trailingIcon: @Composable (() -> Unit)? = null,
    val onTrailingIconClick: () -> Unit = {},
    val title: @Composable (String) -> Unit,
    val subTitle: @Composable ((String) -> Unit)? = null,
    val titleName: String,
    val subTitleName: String
) {
    companion object {
        fun defaults(
            modifier: Modifier = Modifier,
            leadingIcon: @Composable (() -> Unit)? = null,
            onLeadingIconClick: () -> Unit = {},
            trailingIcon: @Composable (() -> Unit)? = null,
            onTrailingIconClick: () -> Unit = {},
            title: @Composable ((String) -> Unit)? = null,
            subTitle: @Composable ((String) -> Unit)? = null,
            titleName: String,
            subTitleName: String,
        ) : TopBarConfiguration {
            return TopBarConfiguration(
                modifier = modifier,
                leadingIcon = leadingIcon ?: {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        tint = Color.Black,
                        contentDescription = "Back"
                    )
                },
                onLeadingIconClick = onLeadingIconClick,
                trailingIcon = trailingIcon ?: {
                    Icon(
                        imageVector = Icons.Default.List,
                        tint = Color.Black,
                        contentDescription = "Threads List"
                    )
                },
                onTrailingIconClick = onTrailingIconClick,
                title = title ?: {
                    Text(
                        text = "Chat Bot",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = styleTitlesSubheadLine
                    )
                },
                subTitle = subTitle ?: {
                    Text(
                        text = "Ask anything!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = styleBodyFootnote
                    )
                },
                titleName = titleName,
                subTitleName = subTitleName
            )
        }
    }
}