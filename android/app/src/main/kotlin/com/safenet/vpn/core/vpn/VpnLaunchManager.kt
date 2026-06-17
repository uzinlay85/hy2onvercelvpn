package com.safenet.vpn.core.vpn

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VpnLaunchManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun launchVpnApp(protocol: String, configUri: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(configUri)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            // No app installed that can handle this URI
            copyToClipboard(configUri)
            false
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("VPN Config", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard. Please paste in your VPN app (e.g. v2rayNG, Hiddify).", Toast.LENGTH_LONG).show()
    }
}
