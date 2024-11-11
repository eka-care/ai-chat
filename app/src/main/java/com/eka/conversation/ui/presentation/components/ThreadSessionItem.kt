package com.eka.conversation.ui.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.eka.conversation.common.Utils
import com.eka.conversation.data.local.db.entities.MessageEntity
import com.eka.conversation.data.local.db.entities.models.MessageContent
import com.eka.conversation.data.local.db.entities.models.MessageRole
import com.eka.conversation.ui.theme.Gray100
import com.eka.conversation.ui.theme.Gray200
import com.eka.conversation.ui.theme.styleBodyFootnote
import com.eka.conversation.ui.theme.styleTitlesSubheadLine

@Composable
fun ThreadSessionItem(
    messageContent: MessageContent,
    onThreadItemClick : () -> Unit
) {
    Row(
        modifier = Modifier
            .clickable {
                onThreadItemClick.invoke()
            }
            .fillMaxWidth()
            .padding(4.dp)
            .background(color = Gray200, shape = RoundedCornerShape(8.dp)),
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            text = messageContent.text.toString(),
            color = MaterialTheme.colorScheme.onSurface,
            style = styleTitlesSubheadLine,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}