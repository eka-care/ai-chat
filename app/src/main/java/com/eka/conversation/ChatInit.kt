package com.eka.conversation

import android.content.Context
import android.content.Intent
import com.eka.conversation.common.models.ChatInitConfiguration
import com.eka.conversation.common.models.NetworkConfiguration
import com.eka.conversation.ui.presentation.activities.ConversationActivity

object ChatInit {
    private var configuration : ChatInitConfiguration? = null

    fun initialize(
        chatInitConfiguration : ChatInitConfiguration,
        context : Context
    ) {
        configuration = chatInitConfiguration
        context.startActivity(Intent(context,ConversationActivity::class.java))
    }

    fun getChatInitConfiguration() : ChatInitConfiguration {
//        return ChatInitConfiguration(
//            networkConfiguration = NetworkConfiguration(
//                params = hashMapOf(
//                    "d_oid" to "161467756044203",
//                    "d_hash" to "6d36c3ca25abe7d9f34b81727f03d719",
//                    "pt_oid" to "161857870651607",
//                ),
//                baseUrl = "http://lucid-ws.eka.care/",
//                aiBotEndpoint = "doc_chat/v1/stream_chat",
//                headers = hashMapOf(),
//            )
//        )
        if(configuration == null) {
            throw IllegalStateException("Chat Init configuration not initialized")
        }
        return configuration!!
    }
}