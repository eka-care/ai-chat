package com.eka.conversation.matrix

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Credentials persisted by the host between launches (mirrors widget store fields). */
data class StoredCredentials(
    val sessToken: String? = null,
    val refreshToken: String? = null,
    val level: String? = null,
    val userId: String? = null,
)

/**
 * Port of synapse `TokenManager`.
 * - init exchange: POST token-exchange {agent_id, auth_token?, refresh_token?} (no auto-refresh on 401)
 * - refresh:       POST token-exchange?type=refresh {refresh_token}
 * Both are single-flight; every success rotates the bearer and notifies the host.
 */
class TokenManager(
    private val api: MatrixApi,
    private val http: MatrixHttpClient,
    private val agentId: String,
    private var partnerAuthToken: String?,
    seeded: StoredCredentials,
    private val onTokenExchange: (TokenExchangeResponse) -> Unit,
) {
    @Volatile var sessToken: String? = seeded.sessToken; private set
    @Volatile private var refreshToken: String? = seeded.refreshToken
    @Volatile var userId: String? = seeded.userId; private set
    @Volatile var level: String? = seeded.level; private set
    @Volatile private var lastResponse: TokenExchangeResponse? = null

    private val mutex = Mutex()
    private var initInFlight: CompletableDeferred<TokenExchangeResponse>? = null
    private var refreshInFlight: CompletableDeferred<TokenExchangeResponse>? = null

    init {
        sessToken?.let { http.setAuthorization(it) }
    }

    val hasResumableCredentials get() = !sessToken.isNullOrBlank() && !refreshToken.isNullOrBlank()

    /** Exchange only when there is nothing to resume (the transport validates a resumed pair). */
    suspend fun ensureCredentials() {
        if (lastResponse != null || hasResumableCredentials) {
            if (lastResponse == null) MatrixLog.d("token exchange skipped: resuming saved tokens (level=$level)")
            return
        }
        ensureExchanged()
    }

    suspend fun ensureExchanged(): TokenExchangeResponse {
        lastResponse?.let { return it }
        val (deferred, owner) = mutex.withLock {
            initInFlight?.let { it to false } ?: CompletableDeferred<TokenExchangeResponse>().also { initInFlight = it }.let { it to true }
        }
        if (owner) {
            try {
                val r = try {
                    api.exchange(TokenExchangeRequest(agentId, partnerAuthToken, refreshToken))
                } catch (e: MatrixApiException) {
                    // The WebView never actually forwarded the doctor's token (it passed `auth`, the widget reads
                    // `authToken`), so production has always run as guest. If Matrix rejects the partner token,
                    // keep DocAssist usable as a guest instead of hard-failing.
                    if (partnerAuthToken == null || e.status !in 400..499) throw e
                    MatrixLog.w("token exchange: partner token rejected (${e.status}), continuing as guest")
                    partnerAuthToken = null
                    api.exchange(TokenExchangeRequest(agentId, null, null))
                }
                deferred.complete(apply(r))
            } catch (e: Throwable) {
                val tagged = if (e is MatrixApiException && e.isAuthError) e.withReason(RecoveryReason.AUTH_EXPIRED) else e
                deferred.completeExceptionally(tagged)
            } finally {
                mutex.withLock { initInFlight = null }
            }
        }
        return deferred.await()
    }

    suspend fun refresh(): TokenExchangeResponse {
        val token = refreshToken
            ?: throw MatrixApiException(401, "No refresh token available to refresh authentication")
        val (deferred, owner) = mutex.withLock {
            refreshInFlight?.let { it to false } ?: CompletableDeferred<TokenExchangeResponse>().also { refreshInFlight = it }.let { it to true }
        }
        if (owner) {
            try {
                deferred.complete(apply(api.refresh(TokenRefreshRequest(token))))
            } catch (e: Throwable) {
                val tagged = if (e is MatrixApiException && e.isAuthError) e.withReason(RecoveryReason.SESSION_FORBIDDEN) else e
                deferred.completeExceptionally(tagged)
            } finally {
                mutex.withLock { refreshInFlight = null }
            }
        }
        return deferred.await()
    }

    /** Refresh and return the new bearer, or null when refresh is impossible. */
    suspend fun refreshBearer(): String? = try {
        refresh()
        sessToken
    } catch (_: Throwable) {
        null
    }

    /** Drop the exchanged pair AND the partner token -> next start is a clean guest exchange. */
    fun clearTokens() {
        resetExchangeState()
        partnerAuthToken = null
    }

    fun clearAll() {
        resetExchangeState()
        userId = null
        level = null
    }

    private fun resetExchangeState() {
        sessToken = null
        refreshToken = null
        lastResponse = null
        http.clearAuthorization()
    }

    private fun apply(r: TokenExchangeResponse): TokenExchangeResponse {
        MatrixLog.d("token exchange ok: level=${r.level}, partnerTokenSent=${partnerAuthToken != null}, expiresIn=${r.expiresIn}s")
        sessToken = r.sessToken
        refreshToken = r.refreshToken
        userId = r.userId
        level = r.level
        lastResponse = r
        http.setAuthorization(r.sessToken)
        onTokenExchange(r)
        return r
    }
}
