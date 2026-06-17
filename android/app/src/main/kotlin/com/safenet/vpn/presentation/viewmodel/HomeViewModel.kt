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
import com.safenet.vpn.data.remote.VercelApiService
import com.safenet.vpn.core.vpn.VpnLaunchManager

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
    private val vercelApiService: VercelApiService,
    private val vpnLaunchManager: VpnLaunchManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Mock fetch from Vercel API
        fetchServers()
    }

    fun fetchServers() {
        viewModelScope.launch {
            try {
                val res = vercelApiService.getServers()
                if (res.isSuccessful && res.body()?.success == true) {
                    val dtos = res.body()?.servers ?: emptyList()
                    val domainServers = dtos.map {
                        FreeServer(it.id, it.name, it.protocol, it.config)
                    }
                    _uiState.update { 
                        it.copy(
                            availableServers = domainServers,
                            selectedServerId = domainServers.firstOrNull()?.id ?: "",
                            selectedServerName = domainServers.firstOrNull()?.name ?: "No Server"
                        )
                    }
                } else {
                    _uiState.update { it.copy(errorMessage = "Failed to load servers") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Network error: ${e.message}") }
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
        val serverId = _uiState.value.selectedServerId
        val server = _uiState.value.availableServers.find { it.id == serverId } ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(vpnState = VpnState.CONNECTING) }
            
            // Launch VPN app using Intent
            val success = vpnLaunchManager.launchVpnApp(server.protocol, server.config)
            
            if (success) {
                // Keep state CONNECTED for visual feedback, though actual VPN is external
                _uiState.update { it.copy(vpnState = VpnState.CONNECTED) }
            } else {
                // If it failed to launch (fallback to copy), we reset state
                _uiState.update { it.copy(vpnState = VpnState.DISCONNECTED) }
            }
        }
    }

    fun disconnect() {
        _uiState.update { it.copy(vpnState = VpnState.DISCONNECTED) }
    }
}
