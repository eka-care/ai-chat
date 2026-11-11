package com.eka.conversation.matrix

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Port of synapse `HttpClient`: fixed Matrix headers, bearer auth, exponential
 * retry on transport failures, and ONE refresh-and-retry on a 401.
 */
class MatrixHttpClient(
    baseUrl: String,
    agentId: String,
    channel: String,
    val okHttp: OkHttpClient = defaultOkHttp(),
) {
    private val baseUrl = baseUrl.trimEnd('/')
    private val headers = linkedMapOf(
        "Content-Type" to "application/json",
        "X-Timezone" to TimeZone.getDefault().id,
        "x-channel" to channel,
        "X-agent-id" to agentId,
    )

    @Volatile
    private var onUnauthorized: (suspend () -> String?)? = null

    fun setAuthorization(token: String) {
        headers["Authorization"] = "Bearer $token"
    }

    fun clearAuthorization() {
        headers.remove("Authorization")
    }

    fun setUnauthorizedHandler(handler: suspend () -> String?) {
        onUnauthorized = handler
    }

    suspend fun <T> get(path: String, serializer: KSerializer<T>, params: Map<String, String>? = null, skipAuthRefresh: Boolean = false): T =
        request("GET", path, null, params, serializer, skipAuthRefresh)

    suspend fun <T> post(path: String, body: String?, serializer: KSerializer<T>, params: Map<String, String>? = null, skipAuthRefresh: Boolean = false): T =
        request("POST", path, body, params, serializer, skipAuthRefresh)

    suspend fun <T> put(path: String, body: String?, serializer: KSerializer<T>): T =
        request("PUT", path, body, null, serializer, false)

    private suspend fun <T> request(
        method: String,
        path: String,
        body: String?,
        params: Map<String, String>?,
        serializer: KSerializer<T>,
        skipAuthRefresh: Boolean,
    ): T {
        try {
            return requestWithRetries(method, path, body, params, serializer)
        } catch (e: MatrixApiException) {
            val handler = onUnauthorized
            if (e.status != 401 || skipAuthRefresh || handler == null) throw e
            handler() ?: throw e
            return requestWithRetries(method, path, body, params, serializer)
        }
    }

    private suspend fun <T> requestWithRetries(
        method: String,
        path: String,
        body: String?,
        params: Map<String, String>?,
        serializer: KSerializer<T>,
        maxRetries: Int = 3,
    ): T {
        var lastError: Throwable? = null
        for (attempt in 0..maxRetries) {
            try {
                return withContext(Dispatchers.IO) { execute(method, path, body, params, serializer) }
            } catch (e: MatrixApiException) {
                throw e
            } catch (e: IOException) {
                lastError = e
                if (attempt == maxRetries) break
                delay((1L shl attempt) * 100L)
            }
        }
        throw MatrixApiException(
            0,
            "Unable to connect to the server. Please check your internet connection.",
            cause = lastError,
        )
    }

    private fun <T> execute(
        method: String,
        path: String,
        body: String?,
        params: Map<String, String>?,
        serializer: KSerializer<T>,
    ): T {
        val url = buildString {
            append(baseUrl).append(path)
            if (!params.isNullOrEmpty()) {
                append("?")
                append(params.entries.joinToString("&") {
                    "${java.net.URLEncoder.encode(it.key, "UTF-8")}=${java.net.URLEncoder.encode(it.value, "UTF-8")}"
                })
            }
        }
        val reqBody = when {
            body != null -> body.toRequestBody(JSON)
            method == "GET" -> null
            else -> "".toRequestBody(JSON)
        }
        val request = Request.Builder().url(url).apply {
            headers.forEach { (k, v) -> header(k, v) }
            method(method, reqBody)
        }.build()
        okHttp.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw toApiError(response, text)
            return try {
                MatrixJson.decodeFromString(serializer, text.ifBlank { "{}" })
            } catch (e: Exception) {
                throw MatrixApiException(502, "Invalid response format from server", cause = e)
            }
        }
    }

    private fun toApiError(response: Response, text: String): MatrixApiException {
        var errBody: ApiErrorBody? = null
        var message = response.message
        try {
            val json = MatrixJson.parseToJsonElement(text) as? JsonObject
            val nested = json?.get("error") as? JsonObject
            errBody = ApiErrorBody(
                code = nested?.str("code") ?: json?.str("code"),
                msg = nested?.str("msg") ?: nested?.str("message") ?: json?.str("msg") ?: json?.str("message"),
            )
            message = errBody.msg ?: message
        } catch (_: Exception) {
        }
        return MatrixApiException(response.code, message.ifBlank { "Request failed (${response.code})" }, errBody)
    }

    /** Current headers (used by the HTTP-stream transport and file upload). */
    fun snapshotHeaders(): Map<String, String> = LinkedHashMap(headers)

    companion object {
        val JSON = "application/json".toMediaType()

        fun defaultOkHttp(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .pingInterval(20, TimeUnit.SECONDS)
            .build()
    }
}
