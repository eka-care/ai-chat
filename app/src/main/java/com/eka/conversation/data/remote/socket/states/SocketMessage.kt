package com.eka.conversation.data.remote.socket.states

import okio.ByteString

sealed class SocketMessage {
    data class TextMessage(val text: String) : SocketMessage()
    data class ByteStringMessage(val bytes: ByteString) : SocketMessage()
}