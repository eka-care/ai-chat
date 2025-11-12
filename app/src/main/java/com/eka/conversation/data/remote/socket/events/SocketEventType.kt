package com.eka.conversation.data.remote.socket.events

import com.google.gson.annotations.SerializedName

enum class SocketEventType(val stringValue: String) {
    @SerializedName("chat")
    CHAT("chat"),

    @SerializedName("stream")
    STREAM("stream"),

    @SerializedName("eos")
    EOS("eos"),

    @SerializedName("auth")
    AUTH("auth"),

    @SerializedName("conn")
    CONN("conn"),

    @SerializedName("err")
    ERR("err"),

    @SerializedName("ping")
    PING("ping"),

    @SerializedName("pong")
    PONG("pong"),

    @SerializedName("sync")
    SYNC("sync")
}