package com.eka.conversation.data.repositories

import com.eka.conversation.client.ChatSDK
import com.eka.conversation.common.models.AuthConfiguration
import com.eka.conversation.data.remote.api.ChatSessionService
import com.eka.conversation.data.remote.models.requests.CreateSessionRequest
import com.eka.conversation.data.remote.models.responses.CreateSessionResponse
import com.eka.conversation.data.remote.models.responses.RefreshTokenResponse
import com.eka.conversation.data.remote.models.responses.SessionStatusResponse
import com.eka.conversation.data.remote.utils.UrlUtils
import com.eka.conversation.domain.repositories.SessionManagementRepository
import com.eka.networking.client.EkaNetwork
import com.haroldadmin.cnradapter.NetworkResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SessionManagementRepositoryImpl(
    private val authConfiguration: AuthConfiguration
) : SessionManagementRepository {

    val matrixService = EkaNetwork.creatorFor(
        appId = ChatSDK.getChatConfiguration().networkConfig.appId,
        service = ChatSessionService.SERVICE_NAME,
    ).create(
        serviceClass = ChatSessionService::class.java,
        serviceUrl = UrlUtils.getMatrixEndpoint()
    )

    val baseHeaders = HashMap<String, String>().apply {
        put("x-agent-id", authConfiguration.agentId)
    }

    override suspend fun createNewSession(userId: String): NetworkResponse<CreateSessionResponse, CreateSessionResponse> =
        withContext(Dispatchers.IO) {
            try {
                val createSessionRequest = CreateSessionRequest(
                    userId = userId
                )
                val response = matrixService.createNewSession(
                    headerMap = baseHeaders,
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
                headers.put("x-agent-id", authConfiguration.agentId)
                val response = matrixService.checkSessionStatus(
                    headerMap = baseHeaders,
                    sessionId = sessionId
                )
                response
            } catch (e: Exception) {
                NetworkResponse.UnknownError(error = e, response = null)
            }
        }

    override suspend fun refreshSessionToken(
        sessionId: String,
        previousSessionToken: String
    ): NetworkResponse<RefreshTokenResponse, RefreshTokenResponse> =
        withContext(Dispatchers.IO) {
            try {
                val headers = baseHeaders.apply {
                    put("x-sess-token", previousSessionToken)
                }
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