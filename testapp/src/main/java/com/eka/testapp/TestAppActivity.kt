package com.eka.testapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eka.conversation.ChatInit
import com.eka.conversation.common.models.ChatInitConfiguration
import com.eka.conversation.common.models.NetworkConfiguration
import com.eka.conversation.ui.presentation.models.BottomSectionConfiguration
import com.eka.conversation.ui.presentation.models.ChatInputAreaConfiguration

class TestAppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ChatInit.initialize(
            chatInitConfiguration = ChatInitConfiguration(
                networkConfiguration = NetworkConfiguration(
                    params = hashMapOf(
                        "d_oid" to "161467756044203",
                        "d_hash" to "6d36c3ca25abe7d9f34b81727f03d719",
                        "pt_oid" to "161857870651607",
                    ),
                    baseUrl = "https://lucid-ws.eka.care/",
                    aiBotEndpoint = "doc_chat/v1/stream_chat",
                    headers = hashMapOf(),
                ),
                bottomSectionConfiguration = BottomSectionConfiguration(
                    chatInputAreaConfiguration = ChatInputAreaConfiguration.defaults(
                        modifier = Modifier
                            .padding(4.dp)
                            .fillMaxWidth(),
                        hint = {
                            Text(
                                text = "Start typing here...",

                                )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = ""
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Send,
                                contentDescription = ""
                            )
                        },
//                        leadingIcon = {
//                            Icon(
//                                painter = painterResource(id = R.drawable.ic_search),
//                                contentDescription = ""
//                            )
//                        },
                    ),
                    trailingIcon = null,
                    isSubmitIconInsideChatInputArea = true
                )
            ),
            context = this
        )
        finish()
    }
}