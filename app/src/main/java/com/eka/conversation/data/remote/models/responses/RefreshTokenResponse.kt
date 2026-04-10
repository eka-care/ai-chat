package com.eka.conversation.data.remote.models.responses

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

// error codes
// validation_error
// session_not_found
// session_token_mismatch

@Keep
data class RefreshTokenResponse(
    @SerializedName("session_id")
    var sessionId: String?,
    @SerializedName("session_token")
    var sessionToken: String?,
    @SerializedName("session_validity_s")
    var sessionValidityS: String?,
    @SerializedName("user_id")
    var userId: String? = null,
    @SerializedName("msg")
    var msg: String? = null,
    @SerializedName("initial_message")
    var initialMessage: InitialMessage? = null,
    @SerializedName("err")
    var err: ResponseError?
)

