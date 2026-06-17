package com.safenet.vpn.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safenet.vpn.core.settings.AppSettingsManager
import com.safenet.vpn.core.settings.ThemeMode
import com.safenet.vpn.core.security.TokenManager
import com.safenet.vpn.data.local.dao.SafeNetDao
import com.safenet.vpn.data.remote.SafeNetApiService
import com.safenet.vpn.data.remote.dto.RefreshTokenRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val autoConnectOnBoot: Boolean = false,
    val isLoggingOut: Boolean = false,
    val isLoggedOut: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val apiService: SafeNetApiService,
    private val tokenManager: TokenManager,
    private val dao: SafeNetDao,
    private val appSettings: AppSettingsManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            themeMode = appSettings.themeMode,
            autoConnectOnBoot = appSettings.autoConnectOnBoot,
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        appSettings.themeMode = mode
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun toggleAutoConnect(enabled: Boolean) {
        appSettings.autoConnectOnBoot = enabled
        _uiState.update { it.copy(autoConnectOnBoot = enabled) }
    }

    fun logout() {
        _uiState.update { it.copy(isLoggingOut = true) }
        viewModelScope.launch {
            try {
                val refresh = tokenManager.getRefreshToken() ?: ""
                if (refresh.isNotEmpty()) {
                    apiService.logout(RefreshTokenRequest(refreshToken = refresh))
                }
            } catch (_: Exception) {}

            tokenManager.clear()
            dao.clearServers()
            dao.clearKeys()

            _uiState.update { it.copy(isLoggingOut = false, isLoggedOut = true) }
        }
    }
}
