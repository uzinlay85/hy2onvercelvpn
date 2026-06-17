package com.safenet.vpn.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safenet.vpn.domain.model.VpnState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FreeServer(
    val id: String,
    val name: String,
    val protocol: String,
    val config: String
)

data class HomeUiState(
    val vpnState: VpnState = VpnState.DISCONNECTED,
    val availableServers: List<FreeServer> = emptyList(),
    val selectedServerId: String = "",
    val selectedServerName: String = "Select Server",
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    // Placeholder for actual VPN Manager/Service
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Mock fetch from Vercel API
        fetchServers()
    }

    private fun fetchServers() {
        viewModelScope.launch {
            // TODO: Replace with actual Retrofit call to Vercel API
            delay(1000)
            val mockServers = listOf(
                FreeServer("1", "Hysteria2 Singapore", "HYSTERIA2", "hysteria2://..."),
                FreeServer("2", "VLESS Japan", "VLESS", "vless://..."),
                FreeServer("3", "Outline USA", "OUTLINE", "ss://...")
            )
            _uiState.update { 
                it.copy(
                    availableServers = mockServers,
                    selectedServerId = mockServers.first().id,
                    selectedServerName = mockServers.first().name
                )
            }
        }
    }

    fun selectServer(id: String) {
        val server = _uiState.value.availableServers.find { it.id == id }
        if (server != null) {
            _uiState.update { 
                it.copy(
                    selectedServerId = server.id,
                    selectedServerName = server.name
                )
            }
        }
    }

    fun connect() {
        viewModelScope.launch {
            _uiState.update { it.copy(vpnState = VpnState.CONNECTING) }
            delay(1500) // Simulate connection delay
            _uiState.update { it.copy(vpnState = VpnState.CONNECTED) }
        }
    }

    fun disconnect() {
        _uiState.update { it.copy(vpnState = VpnState.DISCONNECTED) }
    }
}
