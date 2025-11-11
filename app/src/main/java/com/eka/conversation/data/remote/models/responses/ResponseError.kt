package com.eka.conversation.data.remote.models.responses

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class ResponseError(
    @SerializedName("code")
    val code: String?,
    @SerializedName("msg")
    val msg: String?
)