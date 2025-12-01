package com.eka.conversation.data.remote.socket.events

import com.google.gson.annotations.SerializedName

enum class SocketContentType(val stringValue: String) {
    @SerializedName("text")
    TEXT("text"),

    @SerializedName("audio")
    AUDIO("audio"),

    @SerializedName("file")
    FILE("file"),

    @SerializedName("pill")
    SINGLE_SELECT("pill"),

    @SerializedName("multi")
    MULTI_SELECT("multi"),

    @SerializedName("inline_text")
    INLINE_TEXT("inline_text")
}