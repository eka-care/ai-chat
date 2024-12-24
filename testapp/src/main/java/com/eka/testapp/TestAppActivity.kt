package com.eka.testapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eka.conversation.ChatInit
import com.eka.conversation.common.models.AudioFeatureConfiguration
import com.eka.conversation.common.models.AudioProcessorType
import com.eka.conversation.common.models.ChatGeneralConfiguration
import com.eka.conversation.common.models.ChatInitConfiguration
import com.eka.conversation.common.models.NetworkConfiguration
import com.eka.conversation.ui.presentation.models.BottomSectionConfiguration
import com.eka.conversation.ui.presentation.models.ChatInputAreaConfiguration
import com.eka.conversation.ui.presentation.models.ContentSectionConfiguration
import com.eka.conversation.ui.presentation.models.ThreadScreenConfiguration
import com.eka.conversation.ui.theme.styleTitlesSubheadLine

class TestAppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ChatInit.initialize(
            chatInitConfiguration = ChatInitConfiguration(
                chatGeneralConfiguration = ChatGeneralConfiguration(
                    isChatFirstScreen = false,
                    shouldShowThreadsIconOnChatScreen = false,
                    chatContext = "General Chat",
                    chatSubContext = "Ask Anything!",
                    chatSessionConfig = "",
                    onSessionInvokeNetworkConfiguration = {
                        NetworkConfiguration(
                            params = hashMapOf(),
                            baseUrl = "https://google.com/",
                            aiBotEndpoint = "",
                            headers = hashMapOf(),
                        )
                    },
                    sortBottomSheetLayout = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White),
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "Sort By",
                                style = styleTitlesSubheadLine,
                                color = Color.Black,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                maxLines = 1
                            )
                            Text(
                                text = "Date",
                                style = styleTitlesSubheadLine,
                                color = Color.Black,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                maxLines = 1
                            )
                        }
                    },
                    onSortItemClick = { it ->
                        it
                    },
                    sessionIdentity = null,
                    shouldUseExistingSession = true,
                ),
                audioFeatureConfiguration = AudioFeatureConfiguration(
                    isEnabled = true,
                    audioProcessor = CustomAudioProcessor(),
                    audioProcessorType = AudioProcessorType.GOOGLE_SPEECH_RECOGNIZER
                ),
                networkConfiguration = NetworkConfiguration(
                    params = hashMapOf(),
                    baseUrl = "https://google.com/",
                    aiBotEndpoint = "",
                    headers = hashMapOf(),
                ),
                bottomSectionConfiguration = BottomSectionConfiguration(
                    modifier = Modifier
                        .background(color = Color(0xFFE9EFFF))
                        .padding(12.dp),
                    chatInputAreaConfiguration = ChatInputAreaConfiguration.defaults(
                        modifier = Modifier
                            .height(40.dp)
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
                    ),
                    trailingIcon = null,
                    isSubmitIconInsideChatInputArea = true,
                ),
                threadScreenConfiguration = ThreadScreenConfiguration.defaults(),
                contentSectionConfiguration = ContentSectionConfiguration.defaults(
                    modifier = Modifier
                        .fillMaxSize(),
                    newChatBackground = {
                        Column(
                            modifier = Modifier
                                .fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                modifier = Modifier.clickable {
                                    ChatInit.getChatViewModel()?.sendNewQuery("Suggestion")
                                },
                                text = "Suggestion",
                            )
                        }
                    }
                )
            ),
            context = this
        )
        finish()
    }
}