package com.eka.conversation.data.remote.socket

import com.eka.conversation.common.Utils
import com.eka.conversation.data.remote.socket.events.AuthData
import com.eka.conversation.data.remote.socket.events.AuthEvent
import com.eka.conversation.data.remote.socket.events.BaseSocketEvent
import com.eka.conversation.data.remote.socket.events.SocketEventType
import com.eka.conversation.data.remote.socket.models.UserQueryData

object SocketUtils {
    fun structureEvent(
        eventType: SocketEventType,
        authToken: String,
        userQueryData: UserQueryData? = null
    ): BaseSocketEvent? {
        when (eventType) {
            SocketEventType.AUTH -> {
                return AuthEvent(
                    timeStamp = Utils.getCurrentUTCEpochMillis(),
                    eventType = eventType,
                    eventId = Utils.getCurrentUTCEpochMillis().toString(),
                    data = AuthData(
                        token = "token"
                    )
                )
            }

            SocketEventType.CHAT -> {

            }

            SocketEventType.CONN -> {

            }

            SocketEventType.EOS -> {

            }

            SocketEventType.ERR -> {

            }

            SocketEventType.PING -> {

            }

            SocketEventType.PONG -> {

            }

            SocketEventType.STREAM -> {

            }

            SocketEventType.SYNC -> {

            }
        }
        return null
    }
}