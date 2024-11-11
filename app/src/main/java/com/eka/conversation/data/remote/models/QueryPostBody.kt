package com.eka.conversation.data.remote.models


import com.google.gson.annotations.SerializedName

data class QueryPostBody(
    @SerializedName("messages")
    val messages: List<PostMessage?>?
)