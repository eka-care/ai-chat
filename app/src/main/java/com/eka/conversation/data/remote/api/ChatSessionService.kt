package com.eka.conversation.data.remote.api

import com.eka.conversation.data.remote.models.requests.CreateSessionRequest
import com.eka.conversation.data.remote.models.responses.CreateSessionResponse
import com.eka.conversation.data.remote.models.responses.RefreshTokenResponse
import com.eka.conversation.data.remote.models.responses.SessionStatusResponse
import com.haroldadmin.cnradapter.NetworkResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Path

interface ChatSessionService {
    @POST("med-assist/session")
    suspend fun createNewSession(
        @Body sessionRequest: CreateSessionRequest,
        @HeaderMap headerMap: Map<String, Any>
    ): NetworkResponse<CreateSessionResponse, CreateSessionResponse>

    @GET("med-assist/session/{session_id}")
    suspend fun checkSessionStatus(
        @Path("session_id") sessionId: String,
        @HeaderMap headerMap: Map<String, Any>
    ): NetworkResponse<SessionStatusResponse, SessionStatusResponse>

    @GET("med-assist/session/{session_id}/refresh")
    suspend fun refreshSessionToken(
        @Path("session_id") sessionId: String,
        @HeaderMap headerMap: Map<String, Any>
    ): NetworkResponse<RefreshTokenResponse, RefreshTokenResponse>

    companion object {
        const val SERVICE_NAME = "chat_session_service"
    }
}

//{
//    "error": {
//    "code": "validation_error",
//    "msg": "Session token mismatch"
//}
//}

//{
//    "session_id": "9d58f73c-aca0-4a1c-ac1b-7f989e4126f1",
//    "session_token": "b252bdb14a611e22a74db08a14938083141db02248e744a7e26dd99c083fbf8a",
//    "session_validity_s": 21600
//}