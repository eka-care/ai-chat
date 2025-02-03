package com.eka.testapp

import android.os.Bundle
import androidx.activity.ComponentActivity

class TestAppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        ChatInit.initialize(
//            chatInitConfiguration = ChatInitConfiguration(
//                chatGeneralConfiguration = ChatGeneralConfiguration(
//                    isChatFirstScreen = false,
//                    shouldShowThreadsIconOnChatScreen = false,
//                    chatContext = "General Chat",
//                    chatSubContext = "Ask Anything!",
//                    chatSessionConfig = "",
//                    onSessionInvokeNetworkConfiguration = {
//                        NetworkConfiguration(
//                            params = hashMapOf(),
//                            baseUrl = "https://google.com/",
//                            aiBotEndpoint = "",
//                            headers = hashMapOf(),
//                        )
//                    },
//                    sortBottomSheetLayout = {
//                        Column(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .background(Color.White),
//                            verticalArrangement = Arrangement.Top,
//                            horizontalAlignment = Alignment.Start
//                        ) {
//                            Text(
//                                text = "Sort By",
//                                style = styleTitlesSubheadLine,
//                                color = Color.Black,
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .padding(16.dp),
//                                maxLines = 1
//                            )
//                            Text(
//                                text = "Date",
//                                style = styleTitlesSubheadLine,
//                                color = Color.Black,
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .padding(16.dp),
//                                maxLines = 1
//                            )
//                        }
//                    },
//                    onSortItemClick = { it ->
//                        it
//                    },
////                    sessionIdentity = null,
////                    shouldUseExistingSession = true,
//                ),
//                audioFeatureConfiguration = AudioFeatureConfiguration(
//                    isEnabled = true,
//                    audioProcessor = CustomAudioProcessor(),
//                    audioProcessorType = AudioProcessorType.GOOGLE_SPEECH_RECOGNIZER
//                ),
//                networkConfiguration = NetworkConfiguration(
//                    params = hashMapOf(),
//                    baseUrl = "https://google.com/",
//                    aiBotEndpoint = "",
//                    headers = hashMapOf(),
//                ),
//                bottomSectionConfiguration = BottomSectionConfiguration(
//                    modifier = Modifier
//                        .background(color = Color(0xFFE9EFFF))
//                        .padding(12.dp),
//                    chatInputAreaConfiguration = ChatInputAreaConfiguration.defaults(
//                        modifier = Modifier
//                            .height(40.dp)
//                            .fillMaxWidth(),
//                        hint = {
//                            Text(
//                                text = "Start typing here...",
//                                )
//                        },
//                        leadingIcon = {
//                            Icon(
//                                imageVector = Icons.Default.Search,
//                                contentDescription = ""
//                            )
//                        },
//                        trailingIcon = {
//                            Icon(
//                                imageVector = Icons.Rounded.Send,
//                                contentDescription = ""
//                            )
//                        },
//                    ),
//                    trailingIcon = null,
//                    isSubmitIconInsideChatInputArea = true,
//                ),
//                threadScreenConfiguration = ThreadScreenConfiguration.defaults(),
//                contentSectionConfiguration = ContentSectionConfiguration.defaults(
//                    modifier = Modifier
//                        .fillMaxSize(),
//                    newChatBackground = {
//                        Column(
//                            modifier = Modifier
//                                .fillMaxSize(),
//                            horizontalAlignment = Alignment.CenterHorizontally,
//                            verticalArrangement = Arrangement.Center
//                        ) {
//                            Text(
//                                modifier = Modifier.clickable {
//                                    ChatInit.getChatViewModel()?.sendNewQuery("Suggestion")
//                                },
//                                text = "Suggestion",
//                            )
//                        }
//                    }
//                )
//            ),
//            context = this
//        )
        finish()
    }
}