package com.safenet.vpn.presentation.viewmodel

import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safenet.vpn.BuildConfig
import com.safenet.vpn.core.security.DeviceKeyManager
import com.safenet.vpn.core.security.TokenManager
import com.safenet.vpn.data.remote.SafeNetApiService
import com.safenet.vpn.data.remote.dto.DeviceActivateRequest
import com.safenet.vpn.data.remote.dto.DeviceBootstrapRequest
import com.safenet.vpn.data.remote.dto.AuthResponse
import com.safenet.vpn.data.remote.dto.RefreshTokenRequest
import com.safenet.vpn.data.remote.dto.TokenResponse
import com.safenet.vpn.worker.NotificationSyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.net.UnknownHostException
import java.util.UUID
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val requiresActivation: Boolean = false,
    /** True when bootstrap fails with a network error and the user has an existing
     *  session token. Allows them to skip past the splash and disconnect a stuck VPN. */
    val canSkipToHome: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: SafeNetApiService,
    private val tokenManager: TokenManager,
    private val deviceKeyManager: DeviceKeyManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /** Tracks the active bootstrap coroutine so retries cancel any hung-up previous call. */
    private var bootstrapJob: Job? = null

    /**
     * Returns the stable device fingerprint.
     * ANDROID_ID is tied to (app signing key + device + user account) on Android 8+,
     * so it survives uninstall/reinstall but changes on factory reset.
     */
    fun getOrCreateDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: run {
                // Fallback: generate and persist a random ID (should rarely happen)
                var did = tokenManager.getDeviceId()
                if (did == null) {
                    did = UUID.randomUUID().toString()
                    tokenManager.saveDeviceId(did)
                }
                did
            }
    }

    fun activateDevice(code: String) {
        if (code.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Activation code cannot be empty") }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val req = DeviceActivateRequest(
                    code = code.trim(),
                    fingerprint = getOrCreateDeviceId(),
                    appVersion = BuildConfig.VERSION_NAME,
                    osVersion = android.os.Build.VERSION.RELEASE,
                    name = android.os.Build.MODEL,
                )
                val res = apiService.activateDevice(req)
                if (res.isSuccessful && res.body()?.success == true) {
                    val auth = res.body()?.data
                    if (auth != null) {
                        saveAuthAndSyncNotifications(auth)
                        _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    } else {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Invalid activation response") }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Invalid code or device limit reached") }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Activation failed") }
            }
        }
    }

    fun bootstrapDevice() {
        // Cancel any previously hung-up bootstrap call so the user can always retry.
        bootstrapJob?.cancel()

        _uiState.update { it.copy(isLoading = true, errorMessage = null, requiresActivation = false) }

        bootstrapJob = viewModelScope.launch {
            // Hard 20-second ceiling — the spinner MUST stop one way or another.
            val completed = withTimeoutOrNull(20_000L) {
                try {
                    if (refreshExistingSession()) {
                        _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                        return@withTimeoutOrNull true
                    }

                    val fingerprint = getOrCreateDeviceId()
                    // Build and sign the challenge using the Keystore private key.
                    // If no key exists yet (first install / reinstall), ensureKeyExists() creates one.
                    val challenge = deviceKeyManager.buildChallenge(fingerprint)
                    val signature = deviceKeyManager.sign(challenge)
                    val publicKey = deviceKeyManager.getPublicKeyBase64()

                    val req = DeviceBootstrapRequest(
                        fingerprint = fingerprint,
                        appVersion = BuildConfig.VERSION_NAME,
                        osVersion = android.os.Build.VERSION.RELEASE,
                        name = android.os.Build.MODEL,
                        publicKey = publicKey,
                        signature = signature,
                        challenge = challenge,
                        deviceSecret = null, // v1 legacy — no longer sent
                    )
                    val res = apiService.bootstrapDevice(req)
                    if (res.isSuccessful && res.body()?.success == true) {
                        val auth = res.body()?.data
                        if (auth != null) {
                            saveAuthAndSyncNotifications(auth)
                            _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                        } else {
                            _uiState.update { it.copy(isLoading = false, errorMessage = "Invalid bootstrap response") }
                        }
                    } else {
                        // 403 = Activation code required (premium/restricted device).
                        // 401 = Should not occur with Keystore auth, but handle gracefully.
                        val needsActivation = res.code() == 403
                        val rateLimited = res.code() == 429
                        // Safe read — wrap in runCatching so an exception here cannot crash
                        // the coroutine and leave isLoading = true forever.
                        val hasSession = runCatching {
                            tokenManager.getRefreshToken()?.isNotBlank() == true
                        }.getOrDefault(false)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = if (needsActivation) {
                                    "Device activation is required for this phone."
                                } else if (rateLimited) {
                                    "Too many retry attempts. Please wait a few minutes, then tap Retry now."
                                } else {
                                    "SafeNet server is temporarily unavailable. Tap Retry now to try again."
                                },
                                requiresActivation = needsActivation,
                                canSkipToHome = !needsActivation,
                            )
                        }
                    }
                    true
                } catch (e: CancellationException) {
                    throw e // Let structured concurrency propagate cancellation correctly.
                } catch (e: Exception) {
                    val hasSession = runCatching {
                        tokenManager.getRefreshToken()?.isNotBlank() == true
                    }.getOrDefault(false)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = friendlySetupError(e),
                            requiresActivation = false,
                            canSkipToHome = true,
                        )
                    }
                    true
                }
            }

            // withTimeoutOrNull returned null → 20-second hard ceiling exceeded.
            if (completed == null) {
                val hasSession = runCatching {
                    tokenManager.getRefreshToken()?.isNotBlank() == true
                }.getOrDefault(false)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "SafeNet server is temporarily unreachable. Tap Retry now to try again.",
                        requiresActivation = false,
                        canSkipToHome = true,
                    )
                }
            }
        }
    }

    fun resetState() {
        _uiState.update { AuthUiState() }
    }

    /**
     * Attempts to silently refresh an existing session token.
     * Returns true if refresh succeeded and tokens were saved.
     * Capped at 10 seconds to prevent blocking the spinner indefinitely.
     */
    private suspend fun refreshExistingSession(): Boolean {
        val refreshToken = tokenManager.getRefreshToken()?.takeIf { it.isNotBlank() } ?: return false
        return runCatching {
            withTimeoutOrNull(10_000L) {
                val response = apiService.refreshToken(RefreshTokenRequest(refreshToken))
                if (response.isSuccessful && response.body()?.success == true) {
                    response.body()?.data?.let { saveTokens(it) }
                    response.body()?.data != null
                } else {
                    false
                }
            } ?: false // Timeout → fail silently, proceed to full bootstrap
        }.getOrDefault(false)
    }

    private fun saveTokens(tokens: TokenResponse) {
        tokenManager.saveTokens(tokens.accessToken, tokens.refreshToken)
    }

    private fun saveAuthAndSyncNotifications(auth: AuthResponse) {
        tokenManager.saveTokens(auth.tokens.accessToken, auth.tokens.refreshToken)
        // deviceSecret is no longer issued by the server for Keystore-auth devices.
        // The field is kept in AuthResponse for backward compat with legacy clients.
        auth.deviceSecret?.takeIf { it.isNotBlank() }?.let { tokenManager.saveDeviceSecret(it) }
        tokenManager.saveUserId(auth.user.id)
        auth.user.username?.let { tokenManager.saveUsername(it) }
        auth.user.usedTrafficBytes?.toLongOrNull()?.let { tokenManager.saveUsedTraffic(it) }
        NotificationSyncScheduler.enqueueImmediate(context, "safenet_notification_sync_after_auth")
    }

    private fun friendlySetupError(error: Exception): String {
        return when (error) {
            is UnknownHostException -> "SafeNet server cannot be reached. Tap Retry now to try again."
            is IOException          -> "Network connection is not ready. Tap Retry now to try again."
            else                    -> "SafeNet server is temporarily unavailable. Tap Retry now to try again."
        }
    }
}
