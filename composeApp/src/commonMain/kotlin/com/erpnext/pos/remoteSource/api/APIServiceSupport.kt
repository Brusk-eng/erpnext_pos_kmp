package com.erpnext.pos.remoteSource.api

import com.erpnext.pos.remoteSource.dto.BootstrapRequestDto
import com.erpnext.pos.remoteSource.dto.CustomerDto
import com.erpnext.pos.remoteSource.dto.UserDto
import com.erpnext.pos.utils.TokenUtils
import io.ktor.http.Parameters
import io.ktor.http.formUrlEncode
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

internal data class AuthCodeExchangeRequest(
    val code: String,
    val redirectUri: String,
    val clientId: String,
    val verifier: String,
)

internal fun minimalBootstrapRequest(
    profileName: String? = null,
    limit: Int? = null,
): BootstrapRequestDto =
    BootstrapRequestDto(
        includeInventory = false,
        includeCustomers = false,
        includeInvoices = false,
        includeActivity = false,
        recentPaidOnly = true,
        profileName = profileName,
        limit = limit,
    )

internal fun inventoryBootstrapRequest(limit: Int? = null): BootstrapRequestDto =
    BootstrapRequestDto(
        includeInventory = true,
        includeCustomers = false,
        includeInvoices = false,
        includeActivity = false,
        recentPaidOnly = true,
        limit = limit,
    )

internal fun buildAuthCodeExchangeForm(request: AuthCodeExchangeRequest): String =
    Parameters.build {
          append("grant_type", "authorization_code")
          append("code", request.code)
          append("redirect_uri", request.redirectUri)
          append("client_id", request.clientId)
          append("code_verifier", request.verifier)
        }
        .formUrlEncode()

internal fun decodeIssuer(idToken: String?): String? =
    TokenUtils.decodePayload(idToken.orEmpty())
        ?.get("iss")
        ?.toString()
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.trimEnd('/')
        ?.lowercase()

internal fun UserDto.normalizeForPresentation(
    currentSite: String?,
    fallbackEmail: String?,
): UserDto {
  val normalizedName = name.trim().ifBlank { username?.trim().orEmpty() }
  val normalizedUsername = username?.trim()?.takeIf { it.isNotBlank() } ?: normalizedName
  val normalizedFirstName =
      firstName?.trim()?.takeIf { it.isNotBlank() }
          ?: fullName?.trim()?.substringBefore(" ")?.takeIf { it.isNotBlank() }
          ?: normalizedUsername
  val normalizedEmail =
      email?.trim()?.takeIf { it.isNotBlank() } ?: fallbackEmail ?: normalizedUsername
  val normalizedFullName =
      fullName?.trim()?.takeIf { it.isNotBlank() }
          ?: listOfNotNull(normalizedFirstName, lastName).joinToString(" ").trim()
  return copy(
      name = normalizedName,
      username = normalizedUsername,
      firstName = normalizedFirstName,
      email = normalizedEmail,
      fullName = normalizedFullName,
      image = resolveImageUrl(image, currentSite),
  )
}

internal fun CustomerDto.normalizeForPresentation(currentSite: String?): CustomerDto {
  return copy(image = resolveImageUrl(image, currentSite))
}

internal fun resolveImageUrl(rawImage: String?, currentSite: String?): String? {
  val raw = rawImage?.trim()?.takeIf { it.isNotBlank() } ?: return null
  return when {
    raw.startsWith("http://", ignoreCase = true) ||
        raw.startsWith("https://", ignoreCase = true) -> raw
    raw.startsWith("/") && !currentSite.isNullOrBlank() -> "$currentSite$raw"
    !currentSite.isNullOrBlank() -> "$currentSite/$raw"
    else -> raw
  }
}

internal fun JsonObject.stringOrNull(key: String): String? {
  return this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
}

internal fun JsonObject.intOrNull(key: String): Int? {
  val raw = this[key]?.jsonPrimitive?.contentOrNull ?: return null
  return raw.toIntOrNull()
}

internal fun JsonObject.numberOrNull(key: String): Double? {
  val primitive = this[key]?.jsonPrimitive ?: return null
  return primitive.doubleOrNull ?: primitive.contentOrNull?.toDoubleOrNull()
}

internal fun isSameUserIdentifier(left: String?, right: String?): Boolean {
  val normalizedLeft = left?.trim()?.lowercase()?.takeIf { it.isNotBlank() } ?: return false
  val normalizedRight = right?.trim()?.lowercase()?.takeIf { it.isNotBlank() } ?: return false
  return normalizedLeft == normalizedRight
}
