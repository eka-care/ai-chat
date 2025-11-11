package com.eka.conversation.data.remote.models.responses

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

//{
//    "session_id": "0d06d489-a38f-496e-a998-4a0820ab22ee",
//    "session_validity_s": 21585,
//    "msg": "Session Active"
//}

// In case session not found error will come with code session_not_found then create new session

//{
//    "error": {
//    "code": "session_not_found",
//    "msg": "Session not found with session id: 4dbe9375-e8d8-4ac0-9c14-b7df6069e529"
//}
//}

@Keep
data class SessionStatusResponse(
    @SerializedName("session_id")
    var sessionId: String?,
    @SerializedName("session_validity_s")
    var sessionValidityS: Int?,
    @SerializedName("msg")
    var msg: String?,
    @SerializedName("error")
    var error: ResponseError?
)
