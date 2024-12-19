package com.eka.conversation.ui.presentation.models

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.eka.conversation.ui.theme.styleTitlesSubheadLine

data class ThreadsTopBarConfiguration(
    val modifier: Modifier = Modifier,
    val isSortActive: Boolean = true,
    val isSearchActive: Boolean = true,
    val title: @Composable () -> Unit,
    val subTitle: @Composable (() -> Unit)? = null
) {
    companion object {
        fun defaults(
            modifier: Modifier = Modifier,
            isSortActive: Boolean = true,
            isSearchActive: Boolean = true,
            title: @Composable (() -> Unit)? = null,
            subTitle: @Composable (() -> Unit)? = null
        ): ThreadsTopBarConfiguration {
            return ThreadsTopBarConfiguration(
                modifier = modifier,
                isSortActive = isSortActive,
                isSearchActive = isSearchActive,
                title = title ?: {
                    Text(
                        text = "Chat Sessions",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = styleTitlesSubheadLine,
                    )
                },
                subTitle = subTitle,
            )
        }
    }
}