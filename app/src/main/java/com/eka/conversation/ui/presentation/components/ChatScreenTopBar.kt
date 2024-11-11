package com.eka.conversation.ui.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eka.conversation.ui.presentation.models.TopBarConfiguration

@Composable
fun ChatScreenTopBar(
    modifier: Modifier = Modifier,
    topBarConfiguration: TopBarConfiguration = TopBarConfiguration.defaults()
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        topBarConfiguration.leadingIcon?.let {
            IconButton(onClick = topBarConfiguration.onLeadingIconClick) {
                topBarConfiguration.leadingIcon.invoke()
            }   
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            topBarConfiguration.title.invoke()

            topBarConfiguration.subTitle?.let {
                topBarConfiguration.subTitle.invoke()
            }
        }
        topBarConfiguration.trailingIcon?.let {
            IconButton(onClick = topBarConfiguration.onTrailingIconClick) {
                topBarConfiguration.trailingIcon.invoke()
            }
        }
    }
}