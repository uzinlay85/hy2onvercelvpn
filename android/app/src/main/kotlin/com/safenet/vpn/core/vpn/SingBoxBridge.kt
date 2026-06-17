package com.safenet.vpn.core.vpn

import android.util.Log

/**
 * Bridge to the sing-box native library (libbox.aar).
 *
 * sing-box is loaded from libs/libbox.aar. The actual JNI calls go through
 * the `libbox` package (io.nekohasekai.libbox).
 *
 * If the library is not yet integrated, this stub provides safe fallbacks
 * so the rest of the code compiles and runs without crashing.
 */
object SingBoxBridge {
    private const val TAG = "SingBoxBridge"
    private var running = false

    /**
     * Start the sing-box tunnel.
     * @param tunFd   File descriptor of the TUN interface (from VpnService.Builder.establish())
     * @param config  sing-box JSON configuration string
     */
    fun start(tunFd: Int, config: String) {
        try {
            // When libbox.aar is integrated, replace this with:
            // val service = Libbox.newStandaloneService(config, tunFd)
            // service.start()

            // Try to call libbox via reflection (if .aar is loaded)
            val libboxClass = Class.forName("io.nekohasekai.libbox.Libbox")
            val newServiceMethod = libboxClass.getMethod("newStandaloneService", String::class.java, Int::class.java)
            val serviceInstance = newServiceMethod.invoke(null, config, tunFd)

            val startMethod = serviceInstance!!.javaClass.getMethod("start")
            startMethod.invoke(serviceInstance)

            running = true
            Log.i(TAG, "sing-box started via libbox")

        } catch (e: ClassNotFoundException) {
            // libbox.aar not yet added — log clearly
            Log.e(TAG, "libbox.aar not found! Please add it to android/app/libs/. " +
                    "Download from: https://github.com/MatsuriDayo/libcore/releases", e)
            running = false
            throw RuntimeException("sing-box library (libbox.aar) is missing. " +
                    "Please download it and place it in android/app/libs/libbox.aar")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start sing-box", e)
            running = false
            throw e
        }
    }

    fun stop() {
        if (!running) return
        try {
            val libboxClass = Class.forName("io.nekohasekai.libbox.Libbox")
            val stopMethod = libboxClass.getMethod("stopService")
            stopMethod.invoke(null)
            Log.i(TAG, "sing-box stopped")
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping sing-box (may already be stopped)", e)
        } finally {
            running = false
        }
    }

    fun isRunning() = running
}
