package com.eka.conversation.data.remote.socket

import com.eka.conversation.data.remote.socket.events.BaseSocketEvent
import com.eka.conversation.data.remote.socket.events.SocketEventType
import com.eka.conversation.data.remote.socket.events.receive.ConnectionEvent
import com.eka.conversation.data.remote.socket.events.receive.EndOfStreamEvent
import com.eka.conversation.data.remote.socket.events.receive.ErrorEvent
import com.eka.conversation.data.remote.socket.events.receive.ReceiveChatEvent
import com.eka.conversation.data.remote.socket.events.receive.StreamEvent
import com.eka.conversation.data.remote.socket.events.send.AuthEvent
import com.eka.conversation.data.remote.socket.events.send.SendChatEvent
import com.eka.conversation.data.remote.socket.events.send.SendStreamEvent
import com.google.gson.Gson
import org.json.JSONObject

object SocketEventSerializer {
    fun deserializeReceivedEvent(
        data: String,
    ): BaseSocketEvent? {
        val jsonData = JSONObject(data)
        val eventType = jsonData.optString("ev", "")
        val gson = Gson()
        return when (eventType) {
            SocketEventType.CHAT.stringValue -> {
                gson.fromJson(data, ReceiveChatEvent::class.java)
            }

            SocketEventType.CONN.stringValue -> {
                gson.fromJson(data, ConnectionEvent::class.java)
            }

            SocketEventType.EOS.stringValue -> {
                gson.fromJson(data, EndOfStreamEvent::class.java)
            }

            SocketEventType.ERR.stringValue -> {
                gson.fromJson(data, ErrorEvent::class.java)
            }

            SocketEventType.STREAM.stringValue -> {
                gson.fromJson(data, StreamEvent::class.java)
            }

            else -> {
                null
            }
        }
    }

    fun serializeEvent(
        socketEvent: BaseSocketEvent
    ): String? {
        return when (socketEvent.eventType) {
            SocketEventType.CHAT -> {
                Gson().toJson(socketEvent, SendChatEvent::class.java)
            }

            SocketEventType.STREAM -> {
                Gson().toJson(socketEvent, SendStreamEvent::class.java)
            }

            SocketEventType.AUTH -> {
                Gson().toJson(socketEvent, AuthEvent::class.java)
            }

            else -> {
                null
            }
        }
    }
}