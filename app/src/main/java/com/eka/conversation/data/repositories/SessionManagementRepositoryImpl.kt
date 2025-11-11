package com.eka.conversation.data.repositories

import com.eka.conversation.BuildConfig
import com.eka.conversation.common.Constants
import com.eka.conversation.data.remote.api.ChatSessionService
import com.eka.conversation.data.remote.models.requests.CreateSessionRequest
import com.eka.conversation.data.remote.models.responses.CreateSessionResponse
import com.eka.conversation.data.remote.models.responses.RefreshTokenResponse
import com.eka.conversation.data.remote.models.responses.SessionStatusResponse
import com.eka.conversation.domain.repositories.SessionManagementRepository
import com.eka.networking.client.EkaNetwork
import com.haroldadmin.cnradapter.NetworkResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SessionManagementRepositoryImpl : SessionManagementRepository {

    val matrixService = EkaNetwork.creatorFor(
        appId = Constants.APP_ID,
        service = ChatSessionService.SERVICE_NAME,
    ).create(
        serviceClass = ChatSessionService::class.java,
        serviceUrl = BuildConfig.MATRIX_URL
    )

    override suspend fun createNewSession(userId: String): NetworkResponse<CreateSessionResponse, CreateSessionResponse> =
        withContext(Dispatchers.IO) {
            try {
                val createSessionRequest = CreateSessionRequest(
                    userId = userId
                )
                val headers = HashMap<String, Any>()
                val response = matrixService.createNewSession(
                    headerMap = headers,
                    sessionRequest = createSessionRequest
                )
                response
            } catch (e: Exception) {
                NetworkResponse.UnknownError(error = e, response = null)
            }
        }

    override suspend fun checkSessionStatus(sessionId: String): NetworkResponse<SessionStatusResponse, SessionStatusResponse> =
        withContext(Dispatchers.IO) {
            try {
                val headers = HashMap<String, Any>()
                val response = matrixService.checkSessionStatus(
                    headerMap = headers,
                    sessionId = sessionId
                )
                response
            } catch (e: Exception) {
                NetworkResponse.UnknownError(error = e, response = null)
            }
        }

    override suspend fun refreshSessionToken(sessionId: String): NetworkResponse<RefreshTokenResponse, RefreshTokenResponse> =
        withContext(Dispatchers.IO) {
            try {
                val headers = HashMap<String, Any>()
                val response = matrixService.refreshSessionToken(
                    headerMap = headers,
                    sessionId = sessionId
                )
                response
            } catch (e: Exception) {
                NetworkResponse.UnknownError(error = e, response = null)
            }
        }
}