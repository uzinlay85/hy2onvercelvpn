package com.safenet.vpn.data.remote.dto

import com.safenet.vpn.domain.model.VpnProtocol

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String?,
)

data class AuthResponse(
    val user: UserDto,
    val device: DeviceDto?,
    val tokens: TokenResponse,
    val deviceSecret: String? = null,
)

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
)

data class MessageResponse(
    val success: Boolean,
    val message: String,
)

data class DeviceActivateRequest(
    val code: String,
    val fingerprint: String,
    val platform: String = "ANDROID",
    val appVersion: String? = null,
    val osVersion: String? = null,
    val name: String? = null,
)

data class DeviceBootstrapRequest(
    val fingerprint: String,
    val platform: String = "ANDROID",
    val appVersion: String? = null,
    val osVersion: String? = null,
    val name: String? = null,
    // ── Keystore-based attestation (v2 auth) ─────────────────────────────
    // publicKey:  EC P-256 public key, X.509 DER encoded, Base64 (no-wrap)
    // challenge:  "fingerprint:timestamp_minute" — signed by private key
    // signature:  ECDSA/SHA-256 of challenge, Base64 (no-wrap)
    val publicKey: String? = null,
    val signature: String? = null,
    val challenge: String? = null,
    // ── Legacy (v1 auth) — kept for backward compatibility ────────────────
    val deviceSecret: String? = null,
)

data class RefreshTokenRequest(
    val refreshToken: String,
)

data class DeviceDto(
    val id: String,
    val userId: String,
    val name: String?,
    val fingerprint: String,
    val platform: String,
    val osVersion: String?,
    val appVersion: String?,
    val isTrusted: Boolean,
    val isBanned: Boolean,
    val lastSeenAt: String?,
    val lastSeenIp: String?,
    val createdAt: String?,
    val updatedAt: String?,
)

data class UserDto(
    val id: String,
    val email: String,
    val username: String?,
    val displayName: String?,
    val role: String,
    val status: String,
    val trafficQuotaBytes: String?,
    val usedTrafficBytes: String?,
    val expiresAt: String?,
    val maxDevices: Int,
)

data class ServerDto(
    val id: String,
    val name: String,
    val countryCode: String,
    val countryName: String,
    val city: String?,
    val host: String,
    val port: Int,
    val protocols: List<String>,
    val latencyMs: Int,
    val loadPercent: Int,
    val isRecommended: Boolean,
    val isPremium: Boolean,
    val status: String,
)

data class VpnKeyDto(
    val id: String,
    val name: String?,
    val protocol: String,
    val serverId: String?,
    val status: String,
    val expiresAt: String?,
    val trafficLimitBytes: String?,
    val usedTrafficBytes: String?,
    val deviceLimit: Int,
)

data class KeyConfigResponse(
    val id: String,
    val protocol: String,
    val configType: String,
    val configPayload: String, // encrypted with AES-256 or raw depending on auth
)

data class QrCodeResponse(
    val qrCodeBase64: String,
    val uri: String,
)

data class VpnConnectionRequest(
    val fingerprint: String,
    val platform: String = "ANDROID",
    val appVersion: String? = null,
    val osVersion: String? = null,
    val name: String? = null,
    val serverId: String? = null,
)

data class VpnConnectResponse(
    val protocol: String,
    val keyId: String,
    val sessionId: String,
    val deviceId: String,
    val serverId: String? = null,
    val serverName: String? = null,
    val configFile: String,
    val keyUsage: VpnKeyUsageDto? = null,
)

data class VpnKeyUsageDto(
    val usedTrafficBytes: String?,
    val amneziaTrafficBytes: String?,
    val bytesIn: String?,
    val bytesOut: String?,
)

data class VpnDisconnectResponse(
    val disconnected: Int,
)

data class VpnTrafficRequest(
    val sessionId: String,
    val bytesIn: Long = 0,
    val bytesOut: Long = 0,
)

data class VpnTrafficResponse(
    val success: Boolean,
    val sessionId: String,
)

data class SafeNetNotificationDto(
    val id: String,
    val title: String,
    val body: String,
    val type: String? = null,
    val data: Map<String, String>? = null,
    val createdAt: String,
    val readAt: String? = null,
    val isRead: Boolean = false,
)

data class PlanDto(
    val id: String,
    val name: String,
    val description: String?,
    val priceUsd: Double,
    val durationDays: Int,
    val trafficGB: Long,
    val deviceLimit: Int,
    val protocols: List<String>,
    val features: List<String>,
    val isPopular: Boolean,
)

data class SessionDto(
    val id: String,
    val deviceId: String,
    val serverId: String,
    val protocol: String,
    val clientIp: String,
    val connectedAt: String?,
    val bytesIn: String?,
    val bytesOut: String?,
)
