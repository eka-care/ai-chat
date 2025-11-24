package com.eka.conversation.data.remote.utils

import com.eka.conversation.BuildConfig
import com.eka.conversation.client.ChatInit
import com.eka.conversation.client.models.Environment

object UrlUtils {
    fun getMatrixEndpoint(): String {
        return if (ChatInit.getChatInitConfiguration().environment == Environment.DEV) {
            BuildConfig.MATRIX_URL_DEV
        } else {
            BuildConfig.MATRIX_URL
        }
    }

    fun buildSocketUrl(sessionId: String): String {
        return if (ChatInit.getChatInitConfiguration().environment == Environment.PROD) {
            "wss://matrix-ws.eka.care/ws/med-assist/session/${sessionId}/"
        } else {
            "wss://matrix-ws.dev.eka.care/ws/med-assist/session/${sessionId}/"
        }
    }
}