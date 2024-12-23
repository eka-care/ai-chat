package com.eka.conversation.ui.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.eka.conversation.ChatInit
import com.eka.conversation.R
import com.eka.conversation.ui.presentation.models.ThreadsTopBarConfiguration

@Composable
fun ThreadScreenTopBar(
    modifier: Modifier = Modifier,
    threadsTopBarConfiguration: ThreadsTopBarConfiguration = ThreadsTopBarConfiguration.defaults(),

    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSortClick: () -> Unit
) {
    val chatInitConfig = ChatInit.getChatInitConfiguration()
    Column {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    tint = Color.Black,
                    contentDescription = "Back"
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                threadsTopBarConfiguration.title.invoke()

                threadsTopBarConfiguration.subTitle?.let {
                    threadsTopBarConfiguration.subTitle.invoke()
                }
            }
            if (chatInitConfig.chatGeneralConfiguration.isSearchEnabled) {
                IconButton(onClick = onSearchClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chat_search),
                        tint = Color.Black,
                        contentDescription = "Back"
                    )
                }
            }
            if (chatInitConfig.chatGeneralConfiguration.isSortEnabled) {
                IconButton(onClick = onSortClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chat_sort),
                        tint = Color.Black,
                        contentDescription = "Back"
                    )
                }
            }
        }
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(color = Color(0xFFD1D1D1))
        )
    }

}