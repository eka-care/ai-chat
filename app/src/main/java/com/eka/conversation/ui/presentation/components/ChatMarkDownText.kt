package com.eka.conversation.ui.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eka.conversation.ui.theme.Gray900
import com.eka.conversation.ui.theme.styleBody2Regular
import dev.jeziellago.compose.markdowntext.MarkdownText

@Composable
fun ChatMarkDownText(
    text : String
){
    MarkdownText(
        modifier = Modifier.padding(16.dp),
        markdown = text,
        style = styleBody2Regular.copy(
            color = Gray900
        ),
        linkColor = Color.Blue,
    )
}