package com.eka.conversation.ui.presentation.models

import androidx.annotation.Keep
import androidx.compose.ui.Modifier

@Keep
data class SearchBarConfiguration(
    val modifier: Modifier = Modifier,
    val onSearchItemClick : () -> Unit = {},
    val onQueryChange : (String) -> Unit = {},
)
