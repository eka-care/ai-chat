package com.eka.conversation.data.local.db.entities.models

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
enum class MessageRole(val role : String) {
    @SerializedName("ai") AI(role = "ai"),
    @SerializedName("user")
    USER(role = "user");

    companion object {
        fun fromRoleName(name: String): MessageRole? {
            return entries.find { it.name == name }
        }
    }
}