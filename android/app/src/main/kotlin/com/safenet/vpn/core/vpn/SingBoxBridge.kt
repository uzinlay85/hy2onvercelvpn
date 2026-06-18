package com.safenet.vpn.core.vpn

import android.content.Context
import android.net.VpnService
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
    fun start(tunFd: Int, config: String, context: Context) {
        try {
            // Try to call libbox via reflection (if .aar is loaded)
            val libboxClass = Class.forName("io.nekohasekai.libbox.Libbox")
            
            // Run setup first to initialize paths and running environment for Android user process
            try {
                val setupMethod = libboxClass.getMethod(
                    "setup",
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    Boolean::class.javaPrimitiveType
                )
                val filesDir = context.filesDir.absolutePath
                val cacheDir = context.cacheDir.absolutePath
                setupMethod.invoke(null, filesDir, cacheDir, cacheDir, true)
                Log.i(TAG, "libbox.setup success: filesDir=$filesDir, cacheDir=$cacheDir")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to call libbox.setup via reflection: ${e.message}", e)
            }

            val platformInterfaceClass = Class.forName("io.nekohasekai.libbox.PlatformInterface")
            
            // Create dynamic proxy to implement PlatformInterface
            val platformInterface = java.lang.reflect.Proxy.newProxyInstance(
                libboxClass.classLoader,
                arrayOf(platformInterfaceClass)
            ) { _, method, args ->
                when (method.name) {
                    "openTun" -> tunFd
                    "useProcFS" -> true
                    "includeAllNetworks" -> false
                    "usePlatformAutoDetectInterfaceControl" -> true
                    "usePlatformDefaultInterfaceMonitor" -> false
                    "usePlatformInterfaceGetter" -> false
                    "underNetworkExtension" -> false
                    "readWIFIState" -> null
                    "writeLog" -> { Log.d(TAG, "[Core] ${args?.get(0)}") ; null }
                    "autoDetectInterfaceControl" -> {
                        val socketFd = args?.get(0) as? Int
                        if (socketFd != null) {
                            val vpnService = context as? VpnService
                            val success = vpnService?.protect(socketFd) ?: false
                            if (!success) {
                                Log.w(TAG, "Failed to protect socket fd=$socketFd")
                            } else {
                                Log.d(TAG, "Successfully protected socket fd=$socketFd")
                            }
                        }
                        null
                    }
                    else -> {
                        when (method.returnType) {
                            Boolean::class.javaPrimitiveType -> false
                            Int::class.javaPrimitiveType -> 0
                            Long::class.javaPrimitiveType -> 0L
                            Short::class.javaPrimitiveType -> 0.toShort()
                            Byte::class.javaPrimitiveType -> 0.toByte()
                            Float::class.javaPrimitiveType -> 0f
                            Double::class.javaPrimitiveType -> 0.0
                            Char::class.javaPrimitiveType -> '\u0000'
                            String::class.java -> ""
                            else -> null
                        }
                    }
                }
            }
            
            val newServiceMethod = libboxClass.getMethod("newService", String::class.java, platformInterfaceClass)
            val serviceInstance = newServiceMethod.invoke(null, config, platformInterface)

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
