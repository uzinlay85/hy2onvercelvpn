package com.safenet.vpn.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.safenet.vpn.core.settings.AppSettingsManager
import com.safenet.vpn.core.settings.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val autoConnectOnBoot: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
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
}
