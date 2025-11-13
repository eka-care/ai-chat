package com.eka.conversation.data.remote.socket.models

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class UserQueryData(
    @SerializedName("query")
    val query: String? = null,
    @SerializedName("tool_use_id")
    val toolUseId: String? = null,
    @SerializedName("file_path")
    val filePath: String? = null
)
