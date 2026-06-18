package com.safenet.vpn.core.vpn

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Build
import android.util.Log
import com.safenet.vpn.domain.model.VpnState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the lifecycle of the VPN tunnel (start, stop, state).
 * Interacts with [VpnTunnelService] and listens for broadcast state changes.
 */
@Singleton
class VpnTunnelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "VpnTunnelManager"

    // Exposed VPN state for ViewModels to observe
    private val _vpnState = MutableStateFlow(VpnState.DISCONNECTED)
    val vpnState: StateFlow<VpnState> = _vpnState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.getStringExtra(VpnTunnelService.EXTRA_STATE)) {
                VpnTunnelService.STATE_CONNECTED -> {
                    Log.i(TAG, "VPN CONNECTED")
                    _vpnState.update { VpnState.CONNECTED }
                }
                VpnTunnelService.STATE_DISCONNECTED -> {
                    Log.i(TAG, "VPN DISCONNECTED")
                    _vpnState.update { VpnState.DISCONNECTED }
                }
                VpnTunnelService.STATE_ERROR -> {
                    val msg = intent.getStringExtra(VpnTunnelService.EXTRA_ERROR_MSG) ?: "Unknown VPN error"
                    Log.e(TAG, "VPN ERROR: $msg")
                    _vpnState.update { VpnState.ERROR }
                    _errorMessage.update { msg }
                }
            }
        }
    }

    init {
        // Register broadcast receiver
        val filter = IntentFilter(VpnTunnelService.BROADCAST_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(stateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(stateReceiver, filter)
        }
    }

    /**
     * Returns an Intent that the calling Activity should pass to
     * startActivityForResult() to request VPN permission from Android.
     * Returns null if permission is already granted.
     */
    fun prepareVpnIntent(): Intent? = VpnService.prepare(context)

    /**
     * Starts the VPN tunnel with the given VPN URI.
     * Caller must ensure VPN permission has been granted first (via prepareVpnIntent).
     *
     * @param vpnUri The VPN URI (hysteria2://, vless://, ss://)
     * @return true if the config was valid and the service was started
     */
    fun connect(vpnUri: String, serverName: String): Boolean {
        val cacheFilePath = context.cacheDir.absolutePath + "/cache.db"
        val configJson = VpnConfigConverter.toSingBoxConfig(vpnUri, cacheFilePath)
        if (configJson == null) {
            _errorMessage.update { "Failed to parse VPN config" }
            _vpnState.update { VpnState.ERROR }
            return false
        }

        _vpnState.update { VpnState.CONNECTING }
        _errorMessage.update { null }

        val serviceIntent = Intent(context, VpnTunnelService::class.java).apply {
            action = VpnTunnelService.ACTION_START
            putExtra(VpnTunnelService.EXTRA_CONFIG, configJson)
            putExtra(VpnTunnelService.EXTRA_SERVER_NAME, serverName)
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            Log.i(TAG, "Connecting to $serverName via: ${vpnUri.substringBefore("://")}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start VPN service: ${e.message}", e)
            _errorMessage.update { "Service start failed: ${e.message}" }
            _vpnState.update { VpnState.ERROR }
            return false
        }
        return true
    }

    /**
     * Stops the active VPN tunnel.
     */
    fun disconnect() {
        val stopIntent = Intent(context, VpnTunnelService::class.java).apply {
            action = VpnTunnelService.ACTION_STOP
        }
        context.startService(stopIntent)
        _vpnState.update { VpnState.DISCONNECTING }
    }

    fun clearError() {
        _errorMessage.update { null }
        if (_vpnState.value == VpnState.ERROR) {
            _vpnState.update { VpnState.DISCONNECTED }
        }
    }
}
