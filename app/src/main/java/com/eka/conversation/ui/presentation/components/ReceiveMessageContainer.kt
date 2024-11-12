package com.eka.conversation.ui.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eka.conversation.data.local.db.entities.models.MessageContent
import com.eka.conversation.ui.theme.Gray900
import com.eka.conversation.ui.theme.styleBody2Regular

@Composable
fun ReceiveMessageContainer(
    messageContent: MessageContent
) {
    Row(
        modifier = Modifier
            .padding(end = 24.dp)
    ) {
        BorderCard(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(
                topStart = 0.dp,
                topEnd = 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            content = {
                ChatMarkDownText(text = messageContent.text.toString())
            },
            background = Color.White
        )
    }
}