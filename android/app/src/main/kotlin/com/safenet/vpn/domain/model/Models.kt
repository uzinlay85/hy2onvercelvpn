package com.safenet.vpn.domain.model

/**
 * Represents the current state of the VPN connection.
 */
enum class VpnState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR,
}

/**
 * Supported VPN tunnel.
 */
enum class VpnProtocol(val displayName: String) {
    HYSTERIA2("Hysteria2"),
    VLESS("VLESS"),
    OUTLINE("Outline"),
}

/**
 * Domain model for a VPN server.
 */
data class VpnServer(
    val id: String,
    val name: String,
    val countryCode: String,
    val countryName: String,
    val city: String?,
    val host: String,
    val port: Int,
    val protocols: List<VpnProtocol>,
    val latencyMs: Int,
    val loadPercent: Int,
    val isRecommended: Boolean,
    val isPremium: Boolean,
    val status: ServerStatus,
)

enum class ServerStatus { ONLINE, OFFLINE, MAINTENANCE }

/**
 * Domain model for a VPN key.
 */
data class VpnKey(
    val id: String,
    val name: String?,
    val protocol: VpnProtocol,
    val server: VpnServer?,
    val status: KeyStatus,
    val config: VpnConfig?,
    val expiresAt: Long?,
    val trafficLimitBytes: Long,
    val usedTrafficBytes: Long,
    val deviceLimit: Int,
)

enum class KeyStatus { ACTIVE, REVOKED, EXPIRED, SUSPENDED }

/**
 * Decrypted VPN configuration ready to use with a VPN service.
 */
data class VpnConfig(
    val type: String,
    val uri: String?,
    val accessKey: String?,
    val configFile: String?,   // AmneziaWG
    val rawConfig: String,     // Full JSON
)

/**
 * User domain model.
 */
data class User(
    val id: String,
    val email: String,
    val username: String?,
    val displayName: String?,
    val role: String,
    val status: String,
    val trafficQuotaBytes: Long,
    val usedTrafficBytes: Long,
    val expiresAt: Long?,
    val maxDevices: Int,
)

/**
 * Domain model for a locally saved external AmneziaWG key.
 * Used for multi-key list display in the UI.
 */
data class SavedExternalKey(
    val id: String,
    val name: String,
    val type: String,
    /** Server name/label shown next to the key — e.g. "SG Server 1" */
    val connectedServerName: String?,
    val connectedServerId: String?,
    val isActive: Boolean,
    val createdAt: Long,
)

