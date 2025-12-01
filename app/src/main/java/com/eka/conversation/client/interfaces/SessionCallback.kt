package com.eka.conversation.client.interfaces

import com.eka.conversation.client.models.Message
import com.eka.conversation.common.Response
import com.eka.conversation.data.remote.socket.states.SocketConnectionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SessionCallback {
    fun onSuccess(
        sessionId: String,
        connectionState: StateFlow<SocketConnectionState>,
        sessionMessages: Response<Flow<List<Message>>>,
        queryEnabled: StateFlow<Boolean>
    )

    fun onFailure(error: Exception)
}