package com.eka.conversation.matrix

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Matrix REST resources (port of synapse Auth / Config / Session / ToolCall / Voice resources). */
class MatrixApi(private val http: MatrixHttpClient) {

    // ---- auth (never auto-refreshed: a 401 here is a verdict on the credentials sent)
    suspend fun exchange(body: TokenExchangeRequest): TokenExchangeResponse =
        http.post(AUTH, MatrixJson.encodeToString(body), TokenExchangeResponse.serializer(), skipAuthRefresh = true)

    suspend fun refresh(body: TokenRefreshRequest): TokenExchangeResponse =
        http.post(AUTH, MatrixJson.encodeToString(body), TokenExchangeResponse.serializer(), mapOf("type" to "refresh"), skipAuthRefresh = true)

    // ---- agent config ({success, data} envelope)
    suspend fun agentConfig(agentId: String): AgentConfig? {
        val env = http.get("/med-assist/agent-config/$agentId", AgentConfigEnvelope.serializer())
        return if (env.success != false) env.data else null
    }

    // ---- session
    suspend fun createSession(body: SessionCreateRequest): SessionResponse =
        http.post(SESSION, MatrixJson.encodeToString(body), SessionResponse.serializer())

    suspend fun retrieveSession(sessionId: String, includeMessages: Boolean = false): SessionResponse =
        http.get("$SESSION/$sessionId", SessionResponse.serializer(), if (includeMessages) mapOf("messages" to "true") else null)

    suspend fun pastSessions(): PastSessionsResponse =
        http.get(SESSION, PastSessionsResponse.serializer())

    suspend fun feedback(
        sessionId: String,
        messageId: String? = null,
        feedback: String? = null,
        feedbacks: List<String>? = null,
        reason: String? = null,
        npsBase5: Int? = null,
    ) {
        val path = "$SESSION/$sessionId" + (messageId?.let { "/messages/$it" } ?: "") + "/feedback"
        val body = buildJsonObject {
            feedback?.let { put("feedback", it) }
            feedbacks?.let { list -> put("feedbacks", kotlinx.serialization.json.JsonArray(list.map { kotlinx.serialization.json.JsonPrimitive(it) })) }
            reason?.let { put("feedback_reason", it) }
            npsBase5?.let { put("nps_base5", it) }
        }
        http.put(path, body.toString(), JsonObject.serializer())
    }

    /** POST /session/:id/messages/:msg/tools/:tool -> same shape as a stream tool-call event. */
    suspend fun callTool(sessionId: String, toolId: String, messageId: String, params: JsonObject?): JsonObject {
        val body = buildJsonObject { params?.let { put("tool_params", it) } }
        return http.post("$SESSION/$sessionId/messages/$messageId/tools/$toolId", body.toString(), JsonObject.serializer())
    }

    companion object {
        private const val AUTH = "/med-assist/auth/token-exchange"
        private const val SESSION = "/med-assist/session"
    }
}
