package com.eka.conversation.client.interfaces

import com.eka.conversation.client.models.Message
import kotlinx.coroutines.flow.Flow

interface IResponseStreamHandler {
    fun onSuccess(
        responseStream: Flow<Message?>
    )

    fun onFailure(error: Exception)
}