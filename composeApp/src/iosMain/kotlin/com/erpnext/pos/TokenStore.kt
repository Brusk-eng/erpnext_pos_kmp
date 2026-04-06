package com.erpnext.pos

import com.erpnext.pos.remoteSource.dto.LoginInfo
import com.erpnext.pos.remoteSource.dto.TokenResponse
import com.erpnext.pos.remoteSource.oauth.AuthInfoStore
import com.erpnext.pos.remoteSource.oauth.TokenStore
import com.erpnext.pos.remoteSource.oauth.TransientAuthStore
import com.erpnext.pos.utils.TokenUtils.decodePayload
import com.erpnext.pos.utils.TokenUtils.resolveUserIdFromClaims
import com.erpnext.pos.utils.instanceKeyFromUrl
import kotlinx.cinterop.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.*
import platform.Security.*
import platform.darwin.*

@OptIn(ExperimentalForeignApi::class)
private fun keychainSet(key: String, value: String): Boolean {
  val data = value.cstr.getBytes()
  val query =
      mapOf(kSecClass to kSecClassGenericPassword, kSecAttrAccount to key, kSecValueData to data)
  // delete existing
  SecItemDelete(query.toCFDictionary())
  val status = SecItemAdd(query.toCFDictionary(), null)
  return status == errSecSuccess
}

@OptIn(ExperimentalForeignApi::class)
private fun keychainGet(key: String): String? {
  val query =
      mapOf(
          kSecClass to kSecClassGenericPassword,
          kSecAttrAccount to key,
          kSecReturnData to kCFBooleanTrue,
          kSecMatchLimit to kSecMatchLimitOne,
      )
  val resultPtr = nativeHeap.alloc<COpaquePointerVar>()
  val status = SecItemCopyMatching(query.toCFDictionary(), resultPtr.ptr)
  if (status != errSecSuccess) {
    nativeHeap.free(resultPtr)
    return null
  }
  val data =
      resultPtr.value?.reinterpret<NSData>()
          ?: run {
            nativeHeap.free(resultPtr)
            return null
          }
  val str = NSString.create(data, 0u)!!.toString()
  nativeHeap.free(resultPtr)
  return str
}

@OptIn(ExperimentalForeignApi::class)
private fun keychainDelete(key: String) {
  val query = mapOf(kSecClass to kSecClassGenericPassword, kSecAttrAccount to key)
  SecItemDelete(query.toCFDictionary())
}

class IosTokenStore : TokenStore, TransientAuthStore, AuthInfoStore {
  private val mutex = Mutex()
  private val _flow = MutableStateFlow<TokenResponse?>(null)
  private val json = Json { ignoreUnknownKeys = true }
  private val defaults = NSUserDefaults.standardUserDefaults

  private data class SiteKeys(
      val accessToken: String,
      val refreshToken: String,
      val expires: String,
      val idToken: String,
      val userId: String,
  )

  private companion object {
    const val ACCESS_TOKEN_KEY = "access_token"
    const val REFRESH_TOKEN_KEY = "refresh_token"
    const val EXPIRES_KEY = "expires"
    const val ID_TOKEN_KEY = "id_token"
    const val USER_ID_KEY = "userId"
    const val PKCE_VERIFIER_KEY = "pkce_verifier"
    const val OAUTH_STATE_KEY = "oauth_state"
    const val OAUTH_REDIRECT_URI_KEY = "oauth_redirect_uri"
    const val SITES_INFO_KEY = "sitesInfo"
    const val CURRENT_SITE_KEY = "current_site"
  }

  private fun canonicalUrl(url: String?): String? =
      url?.trim()?.trimEnd('/')?.takeIf { it.isNotBlank() }

  private fun siteScopedKeyForUrl(url: String?, key: String): String {
    val siteKey = instanceKeyFromUrl(url)
    return "${siteKey}_$key"
  }

  private fun siteKeysForUrl(url: String?): SiteKeys =
      SiteKeys(
          accessToken = siteScopedKeyForUrl(url, ACCESS_TOKEN_KEY),
          refreshToken = siteScopedKeyForUrl(url, REFRESH_TOKEN_KEY),
          expires = siteScopedKeyForUrl(url, EXPIRES_KEY),
          idToken = siteScopedKeyForUrl(url, ID_TOKEN_KEY),
          userId = siteScopedKeyForUrl(url, USER_ID_KEY),
      )

  private suspend fun currentSiteKeys(): SiteKeys = siteKeysForUrl(getCurrentSite())

  private fun saveInternal(key: String, value: String) = keychainSet(key, value)

  private fun saveInternal(key: String, value: Long) = keychainSet(key, value.toString())

  private fun loadInternal(key: String) = keychainGet(key)

  private fun deleteInternal(key: String) = keychainDelete(key)

  private fun saveTransient(key: String, value: String) = saveInternal(key, value)

  private fun loadTransient(key: String): String? = loadInternal(key)

  private fun clearTransient(key: String) = deleteInternal(key)

  private fun removeSiteCredentials(keys: SiteKeys) {
    deleteInternal(keys.accessToken)
    deleteInternal(keys.refreshToken)
    deleteInternal(keys.expires)
    deleteInternal(keys.idToken)
    defaults.removeObjectForKey(keys.userId)
  }

  override suspend fun save(tokens: TokenResponse) =
      mutex.withLock {
        val keys = currentSiteKeys()
        val currentAccessToken = loadInternal(keys.accessToken)
        val currentRefreshToken = loadInternal(keys.refreshToken)
        val currentExpiresIn = loadInternal(keys.expires)?.toLongOrNull()
        val currentIdToken = loadInternal(keys.idToken)
        val currentUserId = defaults.stringForKey(keys.userId)

        val mergedAccessToken = tokens.access_token.ifBlank { currentAccessToken.orEmpty() }
        val mergedRefreshToken =
            tokens.refresh_token?.takeIf { it.isNotBlank() }
                ?: currentRefreshToken?.takeIf { it.isNotBlank() }
        val mergedIdToken =
            tokens.id_token?.takeIf { it.isNotBlank() }
                ?: currentIdToken?.takeIf { it.isNotBlank() }
        val mergedExpiresIn = tokens.expires_in ?: currentExpiresIn ?: 0L

        if (mergedAccessToken.isNotBlank()) {
          saveInternal(keys.accessToken, mergedAccessToken)
        } else {
          deleteInternal(keys.accessToken)
        }
        if (mergedRefreshToken == null) {
          deleteInternal(keys.refreshToken)
        } else {
          saveInternal(keys.refreshToken, mergedRefreshToken)
        }
        saveInternal(keys.expires, mergedExpiresIn)
        if (mergedIdToken.isNullOrBlank()) {
          deleteInternal(keys.idToken)
        } else {
          saveInternal(keys.idToken, mergedIdToken)
        }

        val decodedUserId =
            mergedIdToken
                ?.let { resolveUserIdFromClaims(decodePayload(it)) }
                ?.takeIf { it.isNotBlank() } ?: currentUserId
        if (decodedUserId.isNullOrBlank()) {
          defaults.removeObjectForKey(keys.userId)
        } else {
          defaults.setObject(decodedUserId, forKey = keys.userId)
        }
        _flow.value =
            TokenResponse(
                access_token = mergedAccessToken,
                token_type = tokens.token_type,
                expires_in = mergedExpiresIn,
                refresh_token = mergedRefreshToken,
                id_token = mergedIdToken,
                scope = tokens.scope,
            )
      }

  override suspend fun load(): TokenResponse? =
      mutex.withLock {
        val keys = currentSiteKeys()
        val at = loadInternal(keys.accessToken) ?: return null
        val rt = loadInternal(keys.refreshToken)?.takeIf { it.isNotBlank() }
        val expires = loadInternal(keys.expires)?.toLongOrNull()
        val idToken = loadInternal(keys.idToken) ?: return null
        val t =
            TokenResponse(
                access_token = at,
                refresh_token = rt,
                expires_in = expires,
                id_token = idToken,
            )
        _flow.value = t
        t
      }

  override suspend fun clear() =
      mutex.withLock {
        removeSiteCredentials(currentSiteKeys())
        _flow.value = null
      }

  override fun tokensFlow() = _flow.asStateFlow()

  override suspend fun loadUser(): String? = defaults.stringForKey(currentSiteKeys().userId)

  override suspend fun savePkceVerifier(verifier: String) {
    saveTransient(PKCE_VERIFIER_KEY, verifier)
  }

  override suspend fun loadPkceVerifier(): String? = loadTransient(PKCE_VERIFIER_KEY)

  override suspend fun clearPkceVerifier() = clearTransient(PKCE_VERIFIER_KEY)

  override suspend fun saveState(state: String) {
    saveTransient(OAUTH_STATE_KEY, state)
  }

  override suspend fun loadState(): String? = loadTransient(OAUTH_STATE_KEY)

  override suspend fun clearState() = clearTransient(OAUTH_STATE_KEY)

  override suspend fun saveRedirectUri(uri: String) {
    saveTransient(OAUTH_REDIRECT_URI_KEY, uri)
  }

  override suspend fun loadRedirectUri(): String? = loadTransient(OAUTH_REDIRECT_URI_KEY)

  override suspend fun clearRedirectUri() = clearTransient(OAUTH_REDIRECT_URI_KEY)

  // ------------------------------------------------------------
  // AuthInfoStore
  // ------------------------------------------------------------
  override suspend fun loadAuthInfo(): MutableList<LoginInfo> {
    val raw = defaults.stringForKey(SITES_INFO_KEY) ?: return mutableListOf()
    if (raw.isBlank()) return mutableListOf()
    return json.decodeFromString(raw)
  }

  override suspend fun loadAuthInfoByUrl(url: String?, platform: String?): LoginInfo {
    val currentUrl = canonicalUrl(url) ?: canonicalUrl(getCurrentSite())
    val sitesInfo = loadAuthInfo()
    return sitesInfo.first { canonicalUrl(it.url) == currentUrl }
  }

  override suspend fun saveAuthInfo(info: LoginInfo) =
      mutex.withLock {
        val list = loadAuthInfo()
        val canonicalInfoUrl = canonicalUrl(info.url) ?: info.url
        val existing = list.firstOrNull { canonicalUrl(it.url) == canonicalInfoUrl }
        list.removeAll { canonicalUrl(it.url) == canonicalInfoUrl }
        list.add(
            info.copy(
                url = canonicalInfoUrl,
                lastUsedAt = existing?.lastUsedAt,
                isFavorite = existing?.isFavorite ?: info.isFavorite,
            )
        )
        defaults.setObject(json.encodeToString(list), forKey = SITES_INFO_KEY)
        defaults.setObject(canonicalInfoUrl, forKey = CURRENT_SITE_KEY)
      }

  override suspend fun getCurrentSite(): String? = defaults.stringForKey(CURRENT_SITE_KEY)

  override suspend fun deleteSite(url: String): Boolean =
      mutex.withLock {
        val list = loadAuthInfo()
        val canonicalTargetUrl = canonicalUrl(url) ?: url
        if (list.none { canonicalUrl(it.url) == canonicalTargetUrl }) return@withLock false
        val updated = list.filterNot { canonicalUrl(it.url) == canonicalTargetUrl }

        removeSiteCredentials(siteKeysForUrl(canonicalTargetUrl))

        val currentSite = getCurrentSite()
        if (canonicalUrl(currentSite) == canonicalTargetUrl) {
          _flow.value = null
          val nextSite = updated.firstOrNull()?.url
          if (nextSite.isNullOrBlank()) {
            defaults.removeObjectForKey(CURRENT_SITE_KEY)
          } else {
            defaults.setObject(nextSite, forKey = CURRENT_SITE_KEY)
          }
        }

        if (updated.isEmpty()) {
          defaults.removeObjectForKey(SITES_INFO_KEY)
        } else {
          defaults.setObject(json.encodeToString(updated), forKey = SITES_INFO_KEY)
        }
        true
      }

  override suspend fun updateSiteMeta(url: String, lastUsedAt: Long?, isFavorite: Boolean?) =
      mutex.withLock {
        val list = loadAuthInfo()
        val canonicalTargetUrl = canonicalUrl(url) ?: url
        val updated =
            list.map { item ->
              if (canonicalUrl(item.url) != canonicalTargetUrl) return@map item
              item.copy(
                  lastUsedAt = lastUsedAt ?: item.lastUsedAt,
                  isFavorite = isFavorite ?: item.isFavorite,
              )
            }
        defaults.setObject(json.encodeToString(updated), forKey = SITES_INFO_KEY)
      }

  override suspend fun clearAuthInfo() =
      mutex.withLock {
        defaults.removeObjectForKey(SITES_INFO_KEY)
        defaults.removeObjectForKey(CURRENT_SITE_KEY)
      }
}
