package com.eka.conversation.client.interfaces

import com.eka.conversation.client.models.Message

interface ResponseStreamCallback {
    fun onNewEvent(
        event: Message
    )

    fun onComplete()

    fun onSuccess()

    fun onFailure(error: Exception)
}