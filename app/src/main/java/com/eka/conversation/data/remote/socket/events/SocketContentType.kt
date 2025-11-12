package com.eka.conversation.data.remote.socket.events

enum class SocketContentType(val stringValue: String) {
    TEXT("text"),
    AUDIO("audio"),
    JSON("json")
}