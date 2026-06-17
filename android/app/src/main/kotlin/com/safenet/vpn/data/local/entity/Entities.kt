package com.safenet.vpn.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_servers")
data class ServerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val countryCode: String,
    val countryName: String,
    val city: String?,
    val host: String,
    val port: Int,
    val protocols: String, // comma-separated
    val latencyMs: Int,
    val loadPercent: Int,
    val isRecommended: Boolean,
    val isPremium: Boolean,
    val status: String,
)

@Entity(tableName = "cached_keys")
data class KeyEntity(
    @PrimaryKey val id: String,
    val name: String?,
    val protocol: String,
    val serverId: String?,
    val status: String,
    val expiresAt: Long?,
    val trafficLimitBytes: Long,
    val usedTrafficBytes: Long,
    val deviceLimit: Int,
    val configPayload: String?, // Encrypted with local keystore AES
)

/**
 * Locally saved external AmneziaWG keys (user-imported).
 * Config payload is stored separately in EncryptedSharedPreferences keyed by [id].
 * Max 10 entries enforced at ViewModel level.
 */
@Entity(tableName = "external_keys")
data class ExternalKeyEntity(
    @PrimaryKey val id: String,            // UUID
    val name: String,                      // User-provided display name
    val type: String,                      // e.g. "AmneziaWG", "WireGuard"
    val connectedServerId: String?,        // Optional — server ID from SafeNet, if applicable
    val connectedServerName: String?,      // Display: server name e.g. "SG Server 1"
    val isActive: Boolean,                 // Currently selected key
    val createdAt: Long,                   // Epoch ms
)
