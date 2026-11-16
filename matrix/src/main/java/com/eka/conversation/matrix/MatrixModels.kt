package com.eka.conversation.matrix

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/** Shared JSON config for every Matrix payload: tolerant of new server fields. */
val MatrixJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
    encodeDefaults = false
    isLenient = true
}

// ---------------------------------------------------------------- auth

@Serializable
data class TokenExchangeRequest(
    @SerialName("agent_id") val agentId: String,
    @SerialName("auth_token") val authToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
)

@Serializable
data class TokenRefreshRequest(@SerialName("refresh_token") val refreshToken: String)

@Serializable
data class TokenExchangeResponse(
    @SerialName("user_id") val userId: String? = null,
    val level: String? = null, // "user" | "guest"
    @SerialName("sess_token") val sessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long = 0,
    @SerialName("userid_change_detected") val userIdChangeDetected: Boolean? = null,
)

// ---------------------------------------------------------------- session

@Serializable
data class ApiErrorBody(val code: String? = null, val msg: String? = null)

@Serializable
data class InitialSuggestion(
    val label: String? = null,
    val value: String? = null,
    val response: String? = null,
)

@Serializable
data class InitialMessage(
    val text: String? = null,
    val suggestions: List<InitialSuggestion>? = null,
)

@Serializable
data class SessionResponse(
    @SerialName("session_id") val sessionId: String = "",
    @SerialName("user_id") val userId: String? = null,
    val msg: String? = null,
    @SerialName("initial_message") val initialMessage: InitialMessage? = null,
    val error: ApiErrorBody? = null,
    val messages: List<StoredMessage>? = null,
)

@Serializable
data class SessionCreateRequest(
    @SerialName("user_id") val userId: String? = null,
    @SerialName("user_context") val userContext: JsonObject? = null,
    @SerialName("session_context") val sessionContext: JsonObject? = null,
    val referer: String? = null,
)

@Serializable
data class PastSession(
    @SerialName("session_id") val sessionId: String,
    val topic: String? = null,
    val channel: String? = null,
    @SerialName("c_at") val createdAt: String? = null,
    @SerialName("c_ate") val createdAtEpoch: Long? = null,
    @SerialName("last_message") val lastMessage: String? = null,
)

@Serializable
data class PastSessionsResponse(val data: List<PastSession> = emptyList())

@Serializable
data class StoredMessage(
    val id: String,
    val content: String = "",
    val role: String = "assistant",
    /** Epoch SECONDS. */
    val timestamp: Double? = null,
    val elicitations: List<JsonObject>? = null,
)

// ---------------------------------------------------------------- agent config

@Serializable
data class AgentTheme(
    val background: String? = null,
    @SerialName("background_img") val backgroundImg: String? = null,
    val accent: String? = null,
    val mode: String? = null,
    @SerialName("title_img") val titleImg: String? = null,
    @SerialName("icon_img") val iconImg: String? = null,
    val title: String? = null,
    val tagline: String? = null,
    @SerialName("explicit_disclaimer") val explicitDisclaimer: Boolean? = null,
    val tips: List<String>? = null,
    /** Gates chat history UI. */
    val feature: Boolean? = null,
)

@Serializable
data class FeedbackOption(val label: String, val value: String)

@Serializable
data class FeedbackForm(
    val default: List<FeedbackOption>? = null,
    val detractors: List<FeedbackOption>? = null,
    val passives: List<FeedbackOption>? = null,
    val promoters: List<FeedbackOption>? = null,
)

@Serializable
data class AgentFeedbackConfig(
    val type: String = "default", // "default" | "nps"
    val title: String? = null,
    val description: String? = null,
    val form: FeedbackForm? = null,
)

@Serializable
data class AgentConfig(
    val name: String? = null,
    /** Input types the composer offers. null -> all. */
    val allowed: List<String>? = null,
    val connectivity: String? = null, // "http_stream" | "socket"
    val theme: AgentTheme? = null,
    @SerialName("initial_message") val initialMessage: InitialMessage? = null,
    val feedback: AgentFeedbackConfig? = null,
) {
    fun allows(type: String): Boolean = allowed == null || allowed.contains(type)
}

@Serializable
data class AgentConfigEnvelope(val success: Boolean? = null, val data: AgentConfig? = null)

// ---------------------------------------------------------------- tool calls

object MatrixComponents {
    const val MOBILE = "mobile_number"
    const val OTP = "otp"
    const val EMAIL = "email_address"
    const val PILL = "pills"
    const val MULTI = "multi"
    const val DOCTOR_CARD = "doctor_card"
    const val CARD = "card"
    const val MEDIA_CARD = "media_card"

    val AUTH = setOf(MOBILE, OTP, EMAIL)

    /** Components whose entry stays active after SUCCESS (lists keep calling tools). */
    fun staysActive(component: String?) = component == DOCTOR_CARD || component == CARD
}

object ElicitationStatus {
    const val PROGRESS = "progress"
    const val SUCCESS = "success"
    const val FAILURE = "failure"
}

@Serializable
data class SchemaProperty(
    val description: String? = null,
    val pattern: String? = null,
    val minLength: Int? = null,
    val maxLength: Int? = null,
)

@Serializable
data class ToolSchema(
    val properties: Map<String, SchemaProperty>? = null,
    val required: List<String>? = null,
)

@Serializable
data class ToolMeta(
    val schema: ToolSchema? = null,
    @SerialName("tool_result") val toolResult: JsonObject? = null,
    @SerialName("disp_message") val dispMessage: String? = null,
    @SerialName("mcp_meta_fields") val mcpMetaFields: List<String>? = null,
    @SerialName("disp_toast_msg") val dispToastMsg: String? = null,
    val orientation: String? = null,
    @SerialName("user_hint") val userHint: String? = null,
    @SerialName("hide_show_more") val hideShowMore: Boolean? = null,
)

@Serializable
data class ToolDetails(
    val component: String? = null,
    val status: String? = null,
    val input: JsonObject? = null,
    @SerialName("_meta") val meta: ToolMeta? = null,
)

/** `data` of a tool-call event (live socket/stream, or the REST tool-call reply). */
@Serializable
data class ToolCallData(
    @SerialName("tool_id") val toolId: String? = null,
    @SerialName("tool_type") val toolType: String? = null,
    @SerialName("tool_name") val toolName: String? = null,
    val details: ToolDetails? = null,
)

@Serializable
data class PresignedUrl(
    val id: String,
    val url: String,
    val fields: Map<String, String>? = null,
)

// ---------------------------------------------------------------- helpers

fun JsonObject.str(key: String): String? =
    (this[key] as? JsonPrimitive)?.let { if (it.isString || it.contentOrNull != "null") it.contentOrNull else null }

fun JsonElement?.asStringOrNull(): String? = (this as? JsonPrimitive)?.contentOrNull

fun JsonElement?.primitiveContent(): String? = try {
    this?.jsonPrimitive?.contentOrNull
} catch (_: Exception) {
    null
}
