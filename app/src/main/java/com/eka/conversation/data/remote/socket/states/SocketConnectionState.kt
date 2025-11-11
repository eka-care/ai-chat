package com.eka.conversation.data.remote.socket.states

sealed class SocketConnectionState {
    object Connected : SocketConnectionState()
    object Connecting : SocketConnectionState()
    object Disconnecting : SocketConnectionState()
    object Disconnected : SocketConnectionState()
    data class Error(val error: Exception) : SocketConnectionState()
}