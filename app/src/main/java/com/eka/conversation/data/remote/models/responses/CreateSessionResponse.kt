package com.eka.conversation.data.remote.models.responses

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class CreateSessionResponse(
    @SerializedName("session_id")
    var sessionId: String?,
    @SerializedName("session_token")
    var sessionToken: String?,
    @SerializedName("session_validity_s")
    var sessionValidityS: Int?,
    @SerializedName("error")
    var error: ResponseError?
)