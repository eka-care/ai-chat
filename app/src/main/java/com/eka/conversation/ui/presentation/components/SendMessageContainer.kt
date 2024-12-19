package com.eka.conversation.ui.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eka.conversation.data.local.db.entities.models.MessageContent
import com.eka.conversation.data.local.db.entities.models.MessageRole
import com.eka.conversation.ui.theme.ChatSendContainerBg

@Composable
fun SendMessageContainer(
    messageContent: MessageContent
) {
    Row(
        modifier = Modifier
            .padding(start = 24.dp)
    ){
        BorderCard(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 0.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            content = {
                ChatMarkDownText(text = messageContent.text.toString())
            },
            background = ChatSendContainerBg
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BorderCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    elevation: Dp = 0.dp,
    shape: Shape = RoundedCornerShape(16.dp),
    border: BorderStroke = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant),
    background: Color = Color.White,
    content: @Composable () -> Unit
) {
    OutlinedCard(
        modifier = modifier,
        enabled = enabled,
        border = border,
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation
        ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = background
        ),
        onClick = {
            onClick?.invoke()
        },
        content = {
            content.invoke()
        }
    )
}