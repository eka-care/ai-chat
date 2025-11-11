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
    var sessionValidityS: Int?,
    @SerializedName("error")
    var error: ResponseError?
)

