package com.eka.conversation.domain.repositories

import com.eka.conversation.data.remote.models.responses.CreateSessionResponse
import com.eka.conversation.data.remote.models.responses.RefreshTokenResponse
import com.eka.conversation.data.remote.models.responses.SessionStatusResponse
import com.haroldadmin.cnradapter.NetworkResponse

interface SessionManagementRepository {
    suspend fun createNewSession(userId: String): NetworkResponse<CreateSessionResponse, CreateSessionResponse>
    suspend fun checkSessionStatus(sessionId: String): NetworkResponse<SessionStatusResponse, SessionStatusResponse>
    suspend fun refreshSessionToken(
        sessionId: String,
        previousSessionToken: String
    ): NetworkResponse<RefreshTokenResponse, RefreshTokenResponse>
}