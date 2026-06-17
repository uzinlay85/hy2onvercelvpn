package com.safenet.vpn.core.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.util.Log
import com.safenet.vpn.MainActivity
import com.safenet.vpn.R

/**
 * Android VpnService that manages the sing-box VPN tunnel.
 * Started by VpnTunnelManager and communicates state via broadcasts.
 */
class VpnTunnelService : VpnService() {

    companion object {
        const val TAG = "VpnTunnelService"
        const val ACTION_START = "com.safenet.vpn.START"
        const val ACTION_STOP  = "com.safenet.vpn.STOP"
        const val EXTRA_CONFIG = "vpn_config_json"
        const val BROADCAST_STATE = "com.safenet.vpn.STATE"
        const val EXTRA_STATE = "state"
        const val STATE_CONNECTED    = "CONNECTED"
        const val STATE_DISCONNECTED = "DISCONNECTED"
        const val STATE_ERROR        = "ERROR"
        const val EXTRA_ERROR_MSG    = "error_msg"
        private const val NOTIF_CHANNEL = "safenet_vpn"
        private const val NOTIF_ID = 1001
    }

    private var boxService: Any? = null  // sing-box service instance (libbox)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val config = intent.getStringExtra(EXTRA_CONFIG) ?: run {
                    broadcastError("No VPN config provided")
                    stopSelf()
                    return START_NOT_STICKY
                }
                startVpnTunnel(config)
            }
            ACTION_STOP -> {
                stopVpnTunnel()
            }
        }
        return START_STICKY
    }

    private fun startVpnTunnel(configJson: String) {
        try {
            Log.i(TAG, "Starting VPN tunnel...")
            startForeground(NOTIF_ID, buildNotification())

            // Build TUN interface using Android VpnService.Builder
            val builder = Builder()
                .setSession("SafeNet VPN")
                .addAddress("172.19.0.2", 30)
                .addAddress("fdfe:dcba:9876::2", 126)
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")
                .addRoute("0.0.0.0", 0)
                .addRoute("::", 0)
                .setMtu(9000)

            val tunFd = builder.establish()
                ?: throw IllegalStateException("VpnService.Builder.establish() returned null — VPN permission denied?")

            // Start sing-box engine with the TUN fd and config
            SingBoxBridge.start(tunFd.detachFd(), configJson)

            broadcastState(STATE_CONNECTED)
            Log.i(TAG, "VPN tunnel started successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start VPN tunnel", e)
            broadcastError(e.message ?: "Unknown error")
            stopSelf()
        }
    }

    private fun stopVpnTunnel() {
        Log.i(TAG, "Stopping VPN tunnel...")
        try {
            SingBoxBridge.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping sing-box", e)
        }
        broadcastState(STATE_DISCONNECTED)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        runCatching { SingBoxBridge.stop() }
        broadcastState(STATE_DISCONNECTED)
        super.onDestroy()
    }

    // ── Notifications ─────────────────────────────────────────────────────────
    private fun buildNotification(): Notification {
        createNotifChannel()
        val openIntent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stopIntent = Intent(this, VpnTunnelService::class.java).apply { action = ACTION_STOP }
        val stopPi = PendingIntent.getService(this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        return Notification.Builder(this, NOTIF_CHANNEL)
            .setContentTitle("SafeNet VPN")
            .setContentText("VPN tunnel is active")
            .setSmallIcon(R.drawable.ic_safenet_logo)
            .setContentIntent(pi)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, "Disconnect", stopPi).build())
            .build()
    }

    private fun createNotifChannel() {
        val mgr = getSystemService(NotificationManager::class.java)
        if (mgr.getNotificationChannel(NOTIF_CHANNEL) != null) return
        val ch = NotificationChannel(
            NOTIF_CHANNEL,
            "SafeNet VPN",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "VPN connection status"
            setShowBadge(false)
        }
        mgr.createNotificationChannel(ch)
    }

    // ── Broadcast helpers ─────────────────────────────────────────────────────
    private fun broadcastState(state: String) {
        sendBroadcast(Intent(BROADCAST_STATE).apply {
            putExtra(EXTRA_STATE, state)
            setPackage(packageName)
        })
    }

    private fun broadcastError(msg: String) {
        sendBroadcast(Intent(BROADCAST_STATE).apply {
            putExtra(EXTRA_STATE, STATE_ERROR)
            putExtra(EXTRA_ERROR_MSG, msg)
            setPackage(packageName)
        })
    }
}
