package com.eka.conversation.data.remote.models.requests

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class CreateSessionRequest(
    @SerializedName("user_id")
    val userId: String
)
