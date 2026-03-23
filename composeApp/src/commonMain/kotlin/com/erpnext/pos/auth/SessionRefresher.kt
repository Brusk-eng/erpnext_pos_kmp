package com.erpnext.pos.auth

import com.erpnext.pos.navigation.NavRoute
import com.erpnext.pos.navigation.NavigationManager
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.oauth.TokenStore
import com.erpnext.pos.remoteSource.oauth.isRefreshTokenRejected
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.AppSentry
import com.erpnext.pos.utils.NetworkMonitor
import com.erpnext.pos.views.CashBoxManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile

/**
 * Contexto consolidado de sesión.
 *
 * Contiene únicamente datos ya evaluados o preparados para que la policy
 * no tenga que consultar dependencias externas directamente.
 */
data class SessionContext(
    val authInProgress: Boolean,
    val isOnline: Boolean,
    val idToken: String?,
    val accessToken: String?,
    val refreshToken: String?,
    val hasTokens: Boolean,
    val accessTokenPresent: Boolean,
    val secondsLeft: Long?,
    val isNearExpiry: Boolean,
    val issuerMatchesCurrentSite: Boolean?,
)

/**
 * Decisión de negocio sobre el estado actual de la sesión.
 */
sealed interface SessionDecision {
    data class AllowCurrentSession(val reason: String) : SessionDecision
    data class AllowOffline(val reason: String) : SessionDecision
    data class Invalidate(val reason: String) : SessionDecision
    data object Refresh : SessionDecision
}

/**
 * Resultado tipado de la sesión.
 *
 * La fachada pública mantiene Boolean para compatibilidad, pero internamente
 * esto permite más observabilidad y crecimiento futuro.
 */
sealed interface SessionResult {
    data object Valid : SessionResult
    data object Invalidated : SessionResult
    data class AllowedTemporarily(val reason: String) : SessionResult
}

/**
 * Construye el contexto a partir de estado local, conectividad y token store.
 */
class SessionContextProvider(
    private val authFlowState: AuthFlowState?,
    private val networkMonitor: NetworkMonitor,
    private val tokenStore: TokenStore,
    private val apiService: APIService,
    private val refreshThresholdSeconds: Long,
) {
    suspend fun build(): SessionContext {
        val authInProgress = authFlowState?.inProgress?.value == true
        val isOnline = networkMonitor.isConnected.first()
        val tokens = tokenStore.load()

        val idToken = tokens?.id_token
        val accessToken = tokens?.access_token
        val refreshToken = tokens?.refresh_token?.takeIf { it.isNotBlank() }

        val accessTokenPresent = !accessToken.isNullOrBlank()
        val hasTokens =
            !idToken.isNullOrBlank() ||
                    !accessToken.isNullOrBlank() ||
                    !refreshToken.isNullOrBlank()

        val secondsLeft = 4600L //secondsToExpiry(idToken)
        val isNearExpiry = secondsLeft != null && secondsLeft <= refreshThresholdSeconds

        val issuerMatchesCurrentSite =
            if (!idToken.isNullOrBlank()) {
                apiService.isIdTokenIssuerBoundToCurrentSite(idToken)
            } else {
                null
            }

        return SessionContext(
            authInProgress = authInProgress,
            isOnline = isOnline,
            idToken = idToken,
            accessToken = accessToken,
            refreshToken = refreshToken,
            hasTokens = hasTokens,
            accessTokenPresent = accessTokenPresent,
            secondsLeft = secondsLeft,
            isNearExpiry = isNearExpiry,
            issuerMatchesCurrentSite = issuerMatchesCurrentSite,
        )
    }
}

/**
 * Reglas puras del dominio de sesión.
 *
 * No ejecuta side effects.
 */
class SessionPolicy(
    private val isIdTokenValid: (String?) -> Boolean
) {
    fun evaluate(ctx: SessionContext): SessionDecision {
        if (ctx.authInProgress) {
            return SessionDecision.AllowCurrentSession("auth_in_progress")
        }

        if (!ctx.hasTokens) {
            return if (ctx.isOnline) {
                SessionDecision.Invalidate("online_without_tokens")
            } else {
                SessionDecision.AllowOffline("offline_without_tokens")
            }
        }

        if (ctx.issuerMatchesCurrentSite == false) {
            return SessionDecision.Invalidate("issuer_mismatch")
        }

        if (!ctx.isOnline) {
            return SessionDecision.AllowOffline("offline_mode")
        }

        if (isIdTokenValid(ctx.idToken) && !ctx.isNearExpiry) {
            return SessionDecision.AllowCurrentSession("id_token_valid_and_not_near_expiry")
        }

        if (ctx.idToken.isNullOrBlank() && ctx.accessTokenPresent) {
            return SessionDecision.AllowCurrentSession("id_token_missing_but_access_token_present")
        }

        if (ctx.refreshToken.isNullOrBlank()) {
            return if (
                isIdTokenValid(ctx.idToken) ||
                (ctx.idToken.isNullOrBlank() && ctx.accessTokenPresent)
            ) {
                SessionDecision.AllowCurrentSession("no_refresh_token_but_current_session_usable")
            } else {
                SessionDecision.Invalidate("no_refresh_token_and_session_not_usable")
            }
        }

        return SessionDecision.Refresh
    }
}

/**
 * Encapsula la invalidación real de sesión.
 *
 * Aquí concentramos:
 * - limpieza de tokens
 * - cambio del estado invalidated
 * - side effects del dominio/UI
 */
class SessionInvalidator(
    private val tokenStore: TokenStore,
    private val navigationManager: NavigationManager? = null,
    private val cashBoxManager: CashBoxManager? = null,
    private val onInvalidated: suspend () -> Unit = {},
) {
    @Volatile
    var invalidated: Boolean = false
        private set

    suspend fun invalidate(reason: String = "unknown"): SessionResult {
        AppLogger.warn("SessionInvalidator: invalidating session. reason=$reason")

        runCatching { tokenStore.clear() }
            .onFailure { AppLogger.warn("SessionInvalidator: token clear failed", it) }

        runCatching {
            cashBoxManager?.clearContext()
            navigationManager?.navigateTo(NavRoute.Login)
        }.onFailure {
            AppLogger.warn("SessionInvalidator: cash box reset failed", it)
        }

        runCatching { onInvalidated() }
            .onFailure { AppLogger.warn("SessionInvalidator: onInvalidated callback failed", it) }

        invalidated = true
        return SessionResult.Invalidated
    }

    fun markSessionHealthy() {
        invalidated = false
    }
}

/**
 * Ejecuta side effects de acuerdo con la decisión tomada por la policy.
 */
class SessionExecutor(
    private val apiService: APIService,
    private val tokenStore: TokenStore,
    private val invalidator: SessionInvalidator,
    private val isIdTokenValid: (String?) -> Boolean,
) {
    suspend fun execute(decision: SessionDecision, ctx: SessionContext): SessionResult {
        return when (decision) {
            is SessionDecision.AllowCurrentSession -> {
                AppLogger.info("SessionRefresher: allow current session. reason=${decision.reason}")
                invalidator.markSessionHealthy()
                SessionResult.Valid
            }

            is SessionDecision.AllowOffline -> {
                AppLogger.info("SessionRefresher: allow offline. reason=${decision.reason}")
                SessionResult.AllowedTemporarily(decision.reason)
            }

            is SessionDecision.Invalidate -> {
                invalidator.invalidate(decision.reason)
            }

            SessionDecision.Refresh -> {
                refresh(ctx)
            }
        }
    }

    private suspend fun refresh(ctx: SessionContext): SessionResult {
        val refreshToken = ctx.refreshToken
            ?: return invalidator.invalidate("refresh_requested_without_refresh_token")

        return try {
            AppLogger.info(
                "SessionRefresher: refreshing token " +
                        "idTokenExpIn=${ctx.secondsLeft ?: -1}s " +
                        "refreshToken=${maskTokenForLogs(refreshToken)}"
            )

            val refreshed = apiService.refreshToken(refreshToken)
            tokenStore.save(refreshed)

            val refreshedIdToken = refreshed.id_token
            if (
                !refreshedIdToken.isNullOrBlank() &&
                !apiService.isIdTokenIssuerBoundToCurrentSite(refreshedIdToken)
            ) {
                val (issuer, site) = apiService.getIdTokenIssuerAndCurrentSite(refreshedIdToken)
                AppLogger.warn(
                    "SessionRefresher: refreshed token issuer mismatch, invalidating. " +
                            "issuer=$issuer currentSite=$site"
                )
                invalidator.invalidate("issuer_mismatch_after_refresh")
            } else {
                AppLogger.info("SessionRefresher: refresh successful")
                invalidator.markSessionHealthy()
                SessionResult.Valid
            }
        } catch (t: Throwable) {
            handleRefreshFailure(t, ctx)
        }
    }

    private suspend fun handleRefreshFailure(t: Throwable, ctx: SessionContext): SessionResult {
        AppSentry.capture(t, "SessionExecutor.refresh failed")
        AppLogger.warn("SessionExecutor: refresh failed", t)

        return when {
            isRefreshTokenRejected(t) -> {
                AppLogger.warn("SessionExecutor: refresh token rejected by server")
                invalidator.invalidate("refresh_rejected")
            }

            isIdTokenValid(ctx.idToken) -> {
                AppLogger.info("SessionExecutor: refresh failed but id token still valid")
                invalidator.markSessionHealthy()
                SessionResult.Valid
            }

            ctx.idToken.isNullOrBlank() && ctx.accessTokenPresent -> {
                AppLogger.info("SessionExecutor: refresh failed but access token is present")
                invalidator.markSessionHealthy()
                SessionResult.AllowedTemporarily("access_token_present_after_refresh_failure")
            }

            else -> {
                invalidator.invalidate("refresh_failed_and_no_usable_session")
            }
        }
    }
}

/**
 * Fachada final.
 *
 * Mantiene el contrato actual con Boolean para no romper integración existente.
 */
class SessionRefresher(
    private val mutex: Mutex = Mutex(),
    private val contextProvider: SessionContextProvider,
    private val policy: SessionPolicy,
    private val executor: SessionExecutor,
) {
    suspend fun ensureValidSession(): Boolean =
        when (ensureValidSessionResult()) {
            SessionResult.Valid -> true
            is SessionResult.AllowedTemporarily -> true
            SessionResult.Invalidated -> false
        }

    suspend fun ensureValidSessionResult(): SessionResult =
        mutex.withLock {
            val ctx = contextProvider.build()
            val decision = policy.evaluate(ctx)
            executor.execute(decision, ctx)
        }
}

/**
 * Helper de logs para no imprimir tokens completos.
 */
private fun maskTokenForLogs(value: String?): String {
    if (value.isNullOrBlank()) return "<empty>"
    return "len=${value.length} ${value.take(8)}...${value.takeLast(6)}"
}