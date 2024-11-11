package com.eka.conversation.data.remote.models


import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class QueryPostResponse(
    @SerializedName("overwrite")
    val overwrite: Boolean?,
    @SerializedName("text")
    val text: String?
)