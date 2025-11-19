package com.eka.conversation.data.remote.utils

import com.eka.conversation.BuildConfig
import com.eka.conversation.client.ChatInit
import com.eka.conversation.client.Environment

object UrlUtils {
    fun getMatrixEndpoint(): String {
        return if (ChatInit.getChatInitConfiguration().environment == Environment.DEV) {
            BuildConfig.MATRIX_URL_DEV
        } else {
            BuildConfig.MATRIX_URL
        }
    }
}