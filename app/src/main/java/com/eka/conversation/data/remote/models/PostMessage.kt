package com.eka.conversation.data.remote.models


import com.google.gson.annotations.SerializedName

data class PostMessage(
    @SerializedName("role")
    val role: String?,
    @SerializedName("text")
    val text: String?,
    @SerializedName("vault_files")
    val files: List<String>?
)