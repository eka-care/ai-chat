package com.eka.conversation.data.remote.socket.events

import androidx.annotation.Keep

@Keep
interface BaseSocketEvent {
    val timeStamp: Long?
    val eventType: SocketEventType
}