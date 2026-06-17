package com.safenet.vpn.core.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Security checks to prevent running in rooted/emulator environments.
 *
 * These checks are obfuscated during release builds via ProGuard/R8.
 * They serve as a deterrent — determined attackers can bypass them,
 * but they raise the bar significantly.
 */
@Singleton
class SecurityChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "SecurityChecker"
    }

    data class SecurityReport(
        val isRooted: Boolean,
        val isEmulator: Boolean,
        val isDebuggable: Boolean,
        val hasHooks: Boolean,
        val isTampered: Boolean,
        val passed: Boolean,
    )

    fun performSecurityCheck(): SecurityReport {
        val rooted = isRooted()
        val emulator = isEmulator()
        val debuggable = isDebuggable()
        val hooks = hasHookingFramework()
        val tampered = isPackageTampered()

        val passed = !rooted && !emulator && !tampered && !hooks

        if (!passed) {
            Log.w(TAG, "Security check failed: rooted=$rooted emulator=$emulator hooks=$hooks tampered=$tampered")
        }

        return SecurityReport(
            isRooted = rooted,
            isEmulator = emulator,
            isDebuggable = debuggable,
            hasHooks = hooks,
            isTampered = tampered,
            passed = passed,
        )
    }

    // ── Root Detection ────────────────────────────────────────────────────────

    private fun isRooted(): Boolean {
        return checkSuBinary() || checkRootManagementApps() || checkDangerousProps() || checkRWSystem()
    }

    private fun checkSuBinary(): Boolean {
        val suPaths = listOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su",
            "/system/su", "/system/bin/.ext/.su", "/system/usr/we-need-root/su-backup",
            "/data/local/xbin/su", "/data/local/bin/su", "/data/local/su",
            "/system/app/Superuser.apk", "/system/etc/.installed_su_daemon",
        )
        return suPaths.any { File(it).exists() }
    }

    private fun checkRootManagementApps(): Boolean {
        val rootApps = listOf(
            "com.topjohnwu.magisk",
            "com.kingroot.kinguser",
            "com.koushikdutta.superuser",
            "eu.chainfire.supersu",
            "com.noshufou.android.su",
            "com.thirdparty.superuser",
        )
        val pm = context.packageManager
        return rootApps.any { pkg ->
            try { pm.getPackageInfo(pkg, 0); true } catch (e: PackageManager.NameNotFoundException) { false }
        }
    }

    private fun checkDangerousProps(): Boolean {
        val props = mapOf(
            "ro.debuggable" to "1",
            "ro.secure" to "0",
        )
        return props.any { (key, dangerValue) ->
            try {
                val value = Runtime.getRuntime().exec(arrayOf("getprop", key))
                    .inputStream.bufferedReader().readLine()?.trim()
                value == dangerValue
            } catch (e: Exception) { false }
        }
    }

    private fun checkRWSystem(): Boolean {
        return try {
            val mount = File("/proc/mounts").readText()
            mount.contains("/system rw") || mount.contains(" rw,") && mount.contains("/system")
        } catch (e: Exception) { false }
    }

    // ── Emulator Detection ────────────────────────────────────────────────────

    private fun isEmulator(): Boolean {
        val emulatorBuild = Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.startsWith("unknown") ||
            Build.MODEL.contains("google_sdk") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for x86") ||
            Build.MANUFACTURER.contains("Genymotion") ||
            (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
            "google_sdk" == Build.PRODUCT

        val hasEmulatorFiles = File("/dev/socket/qemud").exists() ||
            File("/dev/qemu_pipe").exists() ||
            File("/system/lib/libc_malloc_debug_qemu.so").exists() ||
            File("/sys/qemu_trace").exists()

        return emulatorBuild || hasEmulatorFiles
    }

    // ── Debuggable Check ──────────────────────────────────────────────────────

    private fun isDebuggable(): Boolean {
        return context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
    }

    // ── Hooking Framework Detection (Frida, Xposed) ───────────────────────────

    private fun hasHookingFramework(): Boolean {
        val fridaFiles = listOf(
            "/data/local/tmp/frida-server",
            "/data/local/tmp/re.frida.server",
        )
        val xposedApps = listOf(
            "de.robv.android.xposed.installer",
            "io.github.lsposed.manager",
            "org.lsposed.manager",
        )

        val hasFrida = fridaFiles.any { File(it).exists() }
        val hasXposed = xposedApps.any { pkg ->
            try { context.packageManager.getPackageInfo(pkg, 0); true }
            catch (e: PackageManager.NameNotFoundException) { false }
        }

        // Check for Frida by looking for unusual memory mappings
        val hasFridaMemory = try {
            File("/proc/self/maps").readText().contains("frida")
        } catch (e: Exception) { false }

        return hasFrida || hasXposed || hasFridaMemory
    }

    // ── Package Integrity Check ───────────────────────────────────────────────

    private fun isPackageTampered(): Boolean {
        // In production: compare the app's signing certificate hash
        // against a hardcoded expected hash.
        // This detects APK repacking attacks.
        return try {
            val pm = context.packageManager
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }
            // TODO: Compare actual signature hash with expected hash
            // val sigHash = info.signingInfo.apkContentsSigners[0].toByteArray().toMD5()
            // sigHash != EXPECTED_SIGNATURE_HASH
            false // Remove this line when implementing real check
        } catch (e: Exception) {
            true // If we can't read the signature, treat as tampered
        }
    }
}
