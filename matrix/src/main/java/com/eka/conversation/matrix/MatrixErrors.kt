package com.eka.conversation.matrix

/** What the host must do about an error (mirrors synapse SYNAPSE_RECOVERY_REASON). */
enum class RecoveryReason { AUTH_EXPIRED, SESSION_NOT_FOUND, SESSION_FORBIDDEN, RETRY_LATER }

/** Error classification (mirrors synapse SynapseErrorCode). */
enum class MatrixErrorCode { API, AUTH, SESSION, CONNECTION, MESSAGE, FILE, RECORDING, UNKNOWN }

/** HTTP failure from a Matrix REST call. status 0 = network unreachable. */
class MatrixApiException(
    val status: Int,
    message: String,
    val body: ApiErrorBody? = null,
    val reason: RecoveryReason? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {
    val isAuthError get() = status == 401 || status == 403

    fun withReason(reason: RecoveryReason) = MatrixApiException(status, message ?: "", body, reason, this)
}

/** Unified error surfaced to the UI layer (mirrors synapse TSynapseError). */
data class MatrixError(
    val message: String,
    val code: MatrixErrorCode = MatrixErrorCode.UNKNOWN,
    val reason: RecoveryReason? = null,
    val status: Int? = null,
    val serverCode: String? = null,
) {
    companion object {
        fun from(t: Throwable, code: MatrixErrorCode = MatrixErrorCode.UNKNOWN): MatrixError = when (t) {
            is MatrixApiException -> MatrixError(
                message = t.body?.msg ?: t.message ?: "Something went wrong",
                code = if (t.isAuthError) MatrixErrorCode.AUTH else MatrixErrorCode.API,
                reason = t.reason ?: if (t.isAuthError) RecoveryReason.SESSION_FORBIDDEN else null,
                status = t.status,
                serverCode = t.body?.code,
            )
            else -> MatrixError(t.message ?: "Something went wrong", code)
        }
    }
}

/** Realtime in-band error codes (mirrors SYNAPSE_REALTIME_ERROR_CODES). */
object RealtimeErrorCodes {
    const val SESSION_INACTIVE = "session_not_found"
    const val SESSION_TOKEN_EXPIRED = "sess_token_expired"
    const val INVALID_EVENT = "invalid_event"
    const val INVALID_CONTENT_TYPE = "invalid_content"
    const val PARSING_ERROR = "parsing"
    const val FILE_UPLOAD_INPROGRESS = "file_upload_inprogress"
    const val TIMEOUT = "timeout"
    const val SERVER_ERROR = "server_error"
    const val SESSION_TOKEN_MISMATCH = "session_token_mismatch"
    const val PROMPT_FETCH_ERROR = "prompt_fetch_error"
    const val INVALID_FILE_REQUEST = "invalid_file_request"
    const val SESSION_NOT_OWNED = "session_not_owned"
}
