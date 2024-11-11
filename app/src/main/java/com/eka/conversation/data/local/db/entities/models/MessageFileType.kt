package com.eka.conversation.data.local.db.entities.models

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
enum class MessageFileType {
    @SerializedName("image") IMAGE,
    @SerializedName("audio") AUDIO,
    @SerializedName("any") ANY
}