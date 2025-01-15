package com.eka.conversation.ui.presentation.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.eka.conversation.ChatInit
import com.eka.conversation.ui.presentation.screens.ConversationScreen
import com.eka.conversation.ui.presentation.viewmodels.ChatViewModel

class ConversationActivity : ComponentActivity() {

    private val chatViewModel : ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ChatInit.setChatViewModel(chatViewModel)

        setContent {
            ConversationScreen(
                this,
//                viewModel = chatViewModel,
                onBackPressedDispatcher = onBackPressedDispatcher
            )
        }

        val chatInitConfiguration = ChatInit.getChatInitConfiguration()
    }

    override fun onResume() {
        super.onResume()
    }
}