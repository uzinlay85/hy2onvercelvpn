package com.safenet.vpn.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safenet.vpn.data.local.dao.SafeNetDao
import com.safenet.vpn.data.local.entity.ServerEntity
import com.safenet.vpn.data.remote.SafeNetApiService
import com.safenet.vpn.domain.model.ServerStatus
import com.safenet.vpn.domain.model.VpnProtocol
import com.safenet.vpn.domain.model.VpnServer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServerUiState(
    val isLoading: Boolean = false,
    val servers: List<VpnServer> = emptyList(),
    val filterCountry: String? = null,
    val filterProtocol: String? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class ServerViewModel @Inject constructor(
    private val apiService: SafeNetApiService,
    private val dao: SafeNetDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServerUiState())
    val uiState: StateFlow<ServerUiState> = _uiState.asStateFlow()

    init {
        observeCachedServers()
        fetchServers(forceRefresh = false)
    }

    private fun observeCachedServers() {
        viewModelScope.launch {
            dao.getServersFlow().collect { entities ->
                val list = entities.map { s ->
                    VpnServer(
                        id = s.id,
                        name = s.name,
                        countryCode = s.countryCode,
                        countryName = s.countryName,
                        city = s.city,
                        host = s.host,
                        port = s.port,
                        protocols = s.protocols.split(",").mapNotNull { p ->
                            try { VpnProtocol.valueOf(p) } catch (e: Exception) { null }
                        },
                        latencyMs = s.latencyMs,
                        loadPercent = s.loadPercent,
                        isRecommended = s.isRecommended,
                        isPremium = s.isPremium,
                        status = try { ServerStatus.valueOf(s.status) } catch (e: Exception) { ServerStatus.ONLINE },
                    )
                }
                _uiState.update { it.copy(servers = list) }
            }
        }
    }

    fun fetchServers(forceRefresh: Boolean = true) {
        viewModelScope.launch {
            if (_uiState.value.servers.isEmpty() || forceRefresh) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }
            try {
                val res = apiService.getServers(
                    countryCode = _uiState.value.filterCountry,
                    protocol = _uiState.value.filterProtocol,
                )
                if (res.isSuccessful && res.body()?.success == true) {
                    val dtos = res.body()?.data ?: emptyList()
                    val entities = dtos.map { d ->
                        ServerEntity(
                            id = d.id,
                            name = d.name,
                            countryCode = d.countryCode,
                            countryName = d.countryName,
                            city = d.city,
                            host = d.host,
                            port = d.port,
                            protocols = d.protocols.joinToString(","),
                            latencyMs = d.latencyMs,
                            loadPercent = d.loadPercent,
                            isRecommended = d.isRecommended,
                            isPremium = d.isPremium,
                            status = d.status,
                        )
                    }
                    dao.insertServers(entities)
                    _uiState.update { it.copy(isLoading = false) }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = res.body()?.message ?: "Failed to retrieve servers") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Network error fetching servers") }
            }
        }
    }

    fun setFilterCountry(countryCode: String?) {
        _uiState.update { it.copy(filterCountry = countryCode) }
        fetchServers(forceRefresh = true)
    }

    fun setFilterProtocol(protocol: String?) {
        _uiState.update { it.copy(filterProtocol = protocol) }
        fetchServers(forceRefresh = true)
    }
}
