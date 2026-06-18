package com.safenet.vpn.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safenet.vpn.core.vpn.VpnTunnelManager
import com.safenet.vpn.data.local.dao.SafeNetDao
import com.safenet.vpn.data.local.entity.ServerEntity
import com.safenet.vpn.data.remote.VercelApiService
import com.safenet.vpn.domain.model.VpnState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
    val selectedProtocol: String = "",
    val errorMessage: String? = null,
    val isLoadingServers: Boolean = false,
    val needsVpnPermission: Boolean = false,
    val pendingVpnUri: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val vercelApiService: VercelApiService,
    private val vpnTunnelManager: VpnTunnelManager,
    private val dao: SafeNetDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Observe VpnTunnelManager state and sync to UI
        vpnTunnelManager.vpnState
            .onEach { state -> _uiState.update { it.copy(vpnState = state) } }
            .launchIn(viewModelScope)

        vpnTunnelManager.errorMessage
            .onEach { msg -> if (msg != null) _uiState.update { it.copy(errorMessage = msg) } }
            .launchIn(viewModelScope)

        fetchServers()
    }

    // ── Server fetch ──────────────────────────────────────────────────────────
    fun fetchServers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingServers = true, errorMessage = null) }
            try {
                val res = vercelApiService.getServers()
                if (res.isSuccessful && res.body()?.success == true) {
                    val servers = res.body()?.servers?.map {
                        FreeServer(it.id, it.name, it.protocol, it.config)
                    } ?: emptyList()
                    
                    // Cache to DB
                    dao.clearServers()
                    dao.insertServers(servers.map { ServerEntity(it.id, it.name, it.protocol, it.config, System.currentTimeMillis()) })

                    updateServersList(servers)
                } else {
                    loadCachedServers("Failed to load servers from remote")
                }
            } catch (e: Exception) {
                loadCachedServers("Network error: ${e.message}")
            }
        }
    }

    private suspend fun loadCachedServers(errorMsg: String) {
        val cached = dao.getServersFlow().firstOrNull() ?: emptyList()
        if (cached.isNotEmpty()) {
            val servers = cached.map { FreeServer(it.id, it.name, it.protocol, it.config) }
            updateServersList(servers)
            _uiState.update { it.copy(errorMessage = "Using cached servers ($errorMsg)") }
        } else {
            _uiState.update { it.copy(isLoadingServers = false, errorMessage = errorMsg) }
        }
    }

    private fun updateServersList(servers: List<FreeServer>) {
        _uiState.update {
            it.copy(
                isLoadingServers = false,
                availableServers = servers,
                selectedServerId = servers.firstOrNull()?.id ?: "",
                selectedServerName = servers.firstOrNull()?.name ?: "No Server",
                selectedProtocol = servers.firstOrNull()?.protocol ?: "",
            )
        }
    }

    fun selectServer(id: String) {
        val server = _uiState.value.availableServers.find { it.id == id } ?: return
        _uiState.update {
            it.copy(
                selectedServerId = server.id,
                selectedServerName = server.name,
                selectedProtocol = server.protocol,
            )
        }
    }

    fun selectProtocol(protocolId: String) {
        _uiState.update { it.copy(selectedProtocol = protocolId) }
    }

    fun connect() {
        val server = _uiState.value.availableServers
            .find { it.id == _uiState.value.selectedServerId } ?: return

        val prepareIntent = vpnTunnelManager.prepareVpnIntent()
        if (prepareIntent != null) {
            // Need VPN permission from Android OS — signal Activity
            _uiState.update { it.copy(needsVpnPermission = true, pendingVpnUri = server.config) }
        } else {
            doConnect(server.config, server.name)
        }
    }

    fun onVpnPermissionGranted() {
        val uri = _uiState.value.pendingVpnUri ?: return
        val name = _uiState.value.selectedServerName
        _uiState.update { it.copy(needsVpnPermission = false, pendingVpnUri = null) }
        doConnect(uri, name)
    }

    fun onVpnPermissionDenied() {
        _uiState.update {
            it.copy(
                needsVpnPermission = false,
                pendingVpnUri = null,
                errorMessage = "VPN permission was denied. Please allow it to connect.",
            )
        }
    }

    private fun doConnect(vpnUri: String, serverName: String) {
        val ok = vpnTunnelManager.connect(vpnUri, serverName)
        if (!ok) _uiState.update { it.copy(errorMessage = "Invalid VPN config") }
    }

    fun disconnect() {
        vpnTunnelManager.disconnect()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
        vpnTunnelManager.clearError()
    }
}
