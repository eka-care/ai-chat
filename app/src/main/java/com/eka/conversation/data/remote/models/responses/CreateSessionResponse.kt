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

@Keep
data class InitialMessage(
    @SerializedName("text")
    val text: String? = null,
    @SerializedName("suggestions")
    val suggestions: List<SessionSuggestion>? = null
)

@Keep
data class SessionSuggestion(
    @SerializedName("label")
    val label: String? = null,
    @SerializedName("value")
    val value: String? = null,
    @SerializedName("response")
    val response: String? = null
)