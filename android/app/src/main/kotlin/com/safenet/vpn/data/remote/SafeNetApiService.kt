package com.safenet.vpn.data.remote

import com.safenet.vpn.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API service for SafeNet VPN backend.
 */
interface SafeNetApiService {

    // ── Auth ──────────────────────────────────────────────────────────────────
    @POST("auth/device/activate")
    suspend fun activateDevice(@Body body: DeviceActivateRequest): Response<ApiResponse<AuthResponse>>

    @POST("auth/device/bootstrap")
    suspend fun bootstrapDevice(@Body body: DeviceBootstrapRequest): Response<ApiResponse<AuthResponse>>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body body: RefreshTokenRequest): Response<ApiResponse<TokenResponse>>

    @POST("auth/logout")
    suspend fun logout(@Body body: RefreshTokenRequest): Response<ApiResponse<MessageResponse>>

    @GET("auth/me")
    suspend fun getMe(@Header("Authorization") token: String): Response<ApiResponse<UserDto>>

    // ── Servers ───────────────────────────────────────────────────────────────
    @GET("servers")
    suspend fun getServers(
        @Query("countryCode") countryCode: String? = null,
        @Query("protocol") protocol: String? = null,
    ): Response<ApiResponse<List<ServerDto>>>

    // ── VPN Keys ──────────────────────────────────────────────────────────────
    @GET("vpn-keys/my")
    suspend fun getMyKeys(): Response<ApiResponse<List<VpnKeyDto>>>

    @GET("vpn-keys/{id}/config")
    suspend fun getKeyConfig(@Path("id") keyId: String): Response<ApiResponse<KeyConfigResponse>>

    @GET("vpn-keys/{id}/qr")
    suspend fun getKeyQrCode(@Path("id") keyId: String): Response<ApiResponse<QrCodeResponse>>

    // ── VPN Connection ───────────────────────────────────────────────────────
    @POST("vpn/connect")
    suspend fun connectVpn(
        @Body body: VpnConnectionRequest,
    ): Response<ApiResponse<VpnConnectResponse>>

    @POST("vpn/disconnect")
    suspend fun disconnectVpn(
        @Body body: VpnConnectionRequest,
    ): Response<ApiResponse<VpnDisconnectResponse>>

    @POST("vpn/traffic")
    suspend fun reportTraffic(
        @Body body: VpnTrafficRequest,
    ): Response<ApiResponse<VpnTrafficResponse>>

    // ── Plans ─────────────────────────────────────────────────────────────────
    @GET("plans")
    suspend fun getPlans(): Response<ApiResponse<List<PlanDto>>>

    // ── User ──────────────────────────────────────────────────────────────────
    @GET("users/profile")
    suspend fun getProfile(): Response<ApiResponse<UserDto>>

    @PATCH("users/profile")
    suspend fun updateProfile(@Body body: Map<String, String>): Response<ApiResponse<UserDto>>

    // ── SafeNet Notification Center ─────────────────────────────────────────
    @GET("notifications/my")
    suspend fun getMyNotifications(
        @Query("after") after: String? = null,
        @Query("limit") limit: Int = 50,
    ): Response<ApiResponse<List<SafeNetNotificationDto>>>

    @PATCH("notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: String): Response<ApiResponse<Any>>

    // ── Sessions ──────────────────────────────────────────────────────────────
    @GET("sessions/my")
    suspend fun getMySessions(): Response<ApiResponse<List<SessionDto>>>
}
