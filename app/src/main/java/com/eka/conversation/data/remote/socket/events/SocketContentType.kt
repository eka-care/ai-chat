package com.eka.conversation.data.remote.socket.events

import com.google.gson.annotations.SerializedName

enum class SocketContentType(val stringValue: String) {
    @SerializedName("text")
    TEXT("text"),

    @SerializedName("audio")
    AUDIO("audio"),

    @SerializedName("file")
    FILE("file"),

    @SerializedName("tips")
    TIPS("tips"),

    @SerializedName("tool")
    TOOL("tool"),

    @SerializedName("tool_start")
    TOOL_START("tool_start"),

    @SerializedName("tool_end")
    TOOL_END("tool_end"),

    @SerializedName("pill")
    SINGLE_SELECT("pill"),

    @SerializedName("multi")
    MULTI_SELECT("multi"),

    @SerializedName("inline_text")
    INLINE_TEXT("inline_text")
}