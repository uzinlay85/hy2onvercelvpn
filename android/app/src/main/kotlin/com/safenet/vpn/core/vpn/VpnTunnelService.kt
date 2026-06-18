package com.safenet.vpn.core.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.safenet.vpn.MainActivity
import com.safenet.vpn.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        const val EXTRA_SERVER_NAME = "server_name"
        const val STATE_CONNECTED    = "CONNECTED"
        const val STATE_DISCONNECTED = "DISCONNECTED"
        const val STATE_ERROR        = "ERROR"
        const val EXTRA_ERROR_MSG    = "error_msg"
        private const val NOTIF_CHANNEL = "safenet_vpn"
        private const val NOTIF_ID = 1001
    }

    private var boxService: Any? = null  // sing-box service instance (libbox)
    private var activeTunFd: android.os.ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val config = intent.getStringExtra(EXTRA_CONFIG) ?: run {
                    broadcastError("No VPN config provided")
                    stopSelf()
                    return START_NOT_STICKY
                }
                val serverName = intent.getStringExtra(EXTRA_SERVER_NAME) ?: "Unknown Server"
                startVpnTunnel(config, serverName)
            }
            ACTION_STOP -> {
                stopVpnTunnel()
            }
        }
        return START_STICKY
    }

    private fun startVpnTunnel(configJson: String, serverName: String) {
        try {
            Log.i(TAG, "Starting VPN tunnel...")
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    startForeground(NOTIF_ID, buildNotification(serverName), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                } else {
                    startForeground(NOTIF_ID, buildNotification(serverName))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to startForeground natively: ${e.message}", e)
                try {
                    startForeground(NOTIF_ID, buildNotification(serverName))
                } catch (e2: Exception) {
                    Log.w(TAG, "Failed fallback startForeground: ${e2.message}", e2)
                }
            }

            // Build TUN interface using Android VpnService.Builder
            val builder = Builder()
                .setSession("Zin SafeNet V2")
                .addAddress("172.19.0.2", 30)
                .addAddress("fdfe:dcba:9876::2", 126)
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")
                .addRoute("0.0.0.0", 0)
                .addRoute("::", 0)
                .setMtu(9000)

            activeTunFd = builder.establish()
            if (activeTunFd == null) {
                broadcastError("VPN authorization missing or rejected")
                stopSelf()
                return
            }

            // Start sing-box using our bridge in a background thread!
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // We broadcast connected first, because start() blocks the thread indefinitely!
                    broadcastState(STATE_CONNECTED)
                    Log.i(TAG, "VPN tunnel started successfully")
                    
                    SingBoxBridge.start(activeTunFd!!.fd, configJson, this@VpnTunnelService)
                } catch (e: Exception) {
                    val cause = if (e is java.lang.reflect.InvocationTargetException) e.cause ?: e else e
                    Log.e(TAG, "Failed to start sing-box inside coroutine", cause)
                    broadcastError(cause.message ?: "Failed to start sing-box")
                    stopSelf()
                }
            }

        } catch (e: Exception) {
            val cause = if (e is java.lang.reflect.InvocationTargetException) e.cause ?: e else e
            Log.e(TAG, "Failed to initialize VPN tunnel", cause)
            broadcastError(cause.message ?: "Unknown error")
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
        try {
            activeTunFd?.close()
            activeTunFd = null
        } catch (e: Exception) {
            Log.w(TAG, "Error closing TUN interface", e)
        }
        broadcastState(STATE_DISCONNECTED)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        runCatching { SingBoxBridge.stop() }
        runCatching { activeTunFd?.close(); activeTunFd = null }
        broadcastState(STATE_DISCONNECTED)
        super.onDestroy()
    }

    // ── Notifications ─────────────────────────────────────────────────────────
    private fun buildNotification(serverName: String): Notification {
        createNotifChannel()
        val openIntent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stopIntent = Intent(this, VpnTunnelService::class.java).apply { action = ACTION_STOP }
        val stopPi = PendingIntent.getService(this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        return Notification.Builder(this, NOTIF_CHANNEL)
            .setContentTitle("Zin SafeNet V2")
            .setContentText("Connected to: $serverName")
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
            "Zin SafeNet V2",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "VPN connection status"
            setShowBadge(false)
        }
        mgr.createNotificationChannel(ch)
    }

    // ── Broadcast helpers ─────────────────────────────────────────────────────
    private fun broadcastState(state: String) {
        val intent = Intent(BROADCAST_STATE).apply {
            setPackage(packageName)
            putExtra(EXTRA_STATE, state)
        }
        sendBroadcast(intent)
    }

    private fun broadcastError(msg: String) {
        val intent = Intent(BROADCAST_STATE).apply {
            setPackage(packageName)
            putExtra(EXTRA_STATE, STATE_ERROR)
            putExtra(EXTRA_ERROR_MSG, msg)
        }
        sendBroadcast(intent)
    }
}
