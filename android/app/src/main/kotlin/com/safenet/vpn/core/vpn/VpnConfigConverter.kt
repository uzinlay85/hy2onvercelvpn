package com.safenet.vpn.core.vpn

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder

/**
 * Converts VPN URI strings (hysteria2://, vless://, ss://) into
 * sing-box compatible JSON configuration.
 */
object VpnConfigConverter {

    private const val TAG = "VpnConfigConverter"

    /**
     * Converts a VPN URI to a sing-box JSON config string.
     * @param uri The VPN URI (e.g. "hysteria2://...", "vless://...", "ss://...")
     * @return sing-box JSON config, or null on parse failure
     */
    fun toSingBoxConfig(uri: String, cacheFilePath: String): String? {
        return try {
            val rawConfig = when {
                uri.startsWith("hysteria2://") || uri.startsWith("hy2://") -> buildHysteria2Config(uri)
                uri.startsWith("vless://") -> buildVlessConfig(uri)
                uri.startsWith("ss://") -> buildShadowsocksConfig(uri)
                uri.startsWith("ssconf://") -> buildShadowsocksConfig(uri)
                else -> {
                    Log.w(TAG, "Unknown protocol for URI: $uri")
                    null
                }
            } ?: return null

            // Inject the cache file path dynamically
            val json = JSONObject(rawConfig)
            val experimental = json.optJSONObject("experimental") ?: JSONObject()
            val cacheFile = experimental.optJSONObject("cache_file") ?: JSONObject()
            cacheFile.put("enabled", true)
            cacheFile.put("path", cacheFilePath)
            experimental.put("cache_file", cacheFile)
            json.put("experimental", experimental)

            json.toString(2)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse VPN URI: $uri", e)
            null
        }
    }

    // ── Hysteria2 ──────────────────────────────────────────────────────────────
    private fun buildHysteria2Config(uri: String): String {
        // hysteria2://password@host:port?sni=...&insecure=1#name
        val parsed = URI(uri.replace("hy2://", "hysteria2://"))
        val host = parsed.host ?: throw IllegalArgumentException("Missing host")
        val port = if (parsed.port > 0) parsed.port else 443
        val password = parsed.userInfo?.let { URLDecoder.decode(it, "UTF-8") } ?: ""
        val params = parseQuery(parsed.rawQuery)

        val sni = params["sni"] ?: params["peer"] ?: host
        val insecure = params["insecure"] == "1" || params["allowInsecure"] == "1"
        val obfsPassword = params["obfs-password"] ?: params["obfsParam"]
        val obfsType = params["obfs"]

        val outbound = JSONObject().apply {
            put("type", "hysteria2")
            put("tag", "proxy")
            put("server", host)
            put("server_port", port)
            put("password", password)
            put("tls", JSONObject().apply {
                put("enabled", true)
                put("server_name", sni)
                put("insecure", insecure)
            })
            if (!obfsPassword.isNullOrBlank() && !obfsType.isNullOrBlank()) {
                put("obfs", JSONObject().apply {
                    put("type", obfsType)
                    put("password", obfsPassword)
                })
            }
        }
        return buildFullConfig(outbound)
    }

    // ── VLESS ──────────────────────────────────────────────────────────────────
    private fun buildVlessConfig(uri: String): String {
        // vless://uuid@host:port?type=...&security=...&sni=...#name
        val parsed = URI(uri)
        val host = parsed.host ?: throw IllegalArgumentException("Missing host")
        val port = if (parsed.port > 0) parsed.port else 443
        val uuid = parsed.userInfo ?: throw IllegalArgumentException("Missing UUID")
        val params = parseQuery(parsed.rawQuery)

        val network = params["type"] ?: "tcp"
        val security = params["security"] ?: "none"
        val sni = params["sni"] ?: params["peer"] ?: host
        val flow = params["flow"]
        val fp = params["fp"] ?: "chrome"

        val outbound = JSONObject().apply {
            put("type", "vless")
            put("tag", "proxy")
            put("server", host)
            put("server_port", port)
            put("uuid", uuid)
            if (!flow.isNullOrBlank()) put("flow", flow)

            // Transport
            if (network == "ws") {
                val wsPath = params["path"]?.let { URLDecoder.decode(it, "UTF-8") } ?: "/"
                val wsHost = params["host"]?.let { URLDecoder.decode(it, "UTF-8") } ?: host
                put("transport", JSONObject().apply {
                    put("type", "ws")
                    put("path", wsPath)
                    put("headers", JSONObject().apply { put("Host", wsHost) })
                })
            } else if (network == "grpc") {
                val serviceName = params["serviceName"] ?: params["path"] ?: ""
                put("transport", JSONObject().apply {
                    put("type", "grpc")
                    put("service_name", serviceName)
                })
            }

            // TLS / Reality
            if (security == "tls") {
                put("tls", JSONObject().apply {
                    put("enabled", true)
                    put("server_name", sni)
                    put("utls", JSONObject().apply { put("enabled", true); put("fingerprint", fp) })
                })
            } else if (security == "reality") {
                val pbk = params["pbk"] ?: ""
                val sid = params["sid"] ?: ""
                put("tls", JSONObject().apply {
                    put("enabled", true)
                    put("server_name", sni)
                    put("utls", JSONObject().apply { put("enabled", true); put("fingerprint", fp) })
                    put("reality", JSONObject().apply {
                        put("enabled", true)
                        put("public_key", pbk)
                        put("short_id", sid)
                    })
                })
            }
        }
        return buildFullConfig(outbound)
    }

    // ── Shadowsocks / Outline ─────────────────────────────────────────────────
    private fun buildShadowsocksConfig(uri: String): String {
        // ss://BASE64(method:password)@host:port#name  or  ss://BASE64@host:port#...
        val cleanUri = if (uri.startsWith("ssconf://")) {
            uri.replace("ssconf://", "ss://")
        } else uri

        val withoutPrefix = cleanUri.removePrefix("ss://")
        val fragmentStripped = withoutPrefix.substringBefore("#")
        val atIndex = fragmentStripped.lastIndexOf('@')

        val (method, password, host, port) = if (atIndex >= 0) {
            val userInfoEncoded = fragmentStripped.substring(0, atIndex)
            val hostPort = fragmentStripped.substring(atIndex + 1)
            val h = hostPort.substringBeforeLast(":")
            val p = hostPort.substringAfterLast(":").toIntOrNull() ?: 443
            // Try decoding as base64
            val decoded = try {
                String(android.util.Base64.decode(userInfoEncoded, android.util.Base64.NO_WRAP))
            } catch (_: Exception) {
                userInfoEncoded
            }
            val m = decoded.substringBefore(":")
            val pw = decoded.substringAfter(":")
            listOf(m, pw, h, p)
        } else {
            throw IllegalArgumentException("Invalid ss:// URI format")
        }

        val outbound = JSONObject().apply {
            put("type", "shadowsocks")
            put("tag", "proxy")
            put("server", host)
            put("server_port", port)
            put("method", method)
            put("password", password)
        }
        return buildFullConfig(outbound)
    }

    // ── Full sing-box config template ─────────────────────────────────────────
    private fun buildFullConfig(outbound: JSONObject): String {
        val config = JSONObject().apply {
            put("log", JSONObject().apply {
                put("level", "info")
                put("timestamp", true)
            })
            put("inbounds", JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "tun")
                    put("tag", "tun-in")
                    put("inet4_address", JSONArray().apply { put("172.19.0.2/30") })
                    put("inet6_address", JSONArray().apply { put("fdfe:dcba:9876::2/126") })
                    put("auto_route", false)
                    put("strict_route", false)
                    put("sniff", true)
                    put("sniff_override_destination", false)
                })
            })
            put("outbounds", JSONArray().apply {
                put(outbound)
                put(JSONObject().apply {
                    put("type", "direct")
                    put("tag", "direct")
                })
                put(JSONObject().apply {
                    put("type", "dns")
                    put("tag", "dns-out")
                })
            })
            put("route", JSONObject().apply {
                put("rules", JSONArray().apply {
                    put(JSONObject().apply {
                        put("protocol", "dns")
                        put("outbound", "dns-out")
                    })
                    put(JSONObject().apply {
                        put("geoip", JSONArray().apply { put("private") })
                        put("outbound", "direct")
                    })
                })
                put("final", "proxy")
                put("auto_detect_interface", false)
            })
            put("dns", JSONObject().apply {
                put("servers", JSONArray().apply {
                    put(JSONObject().apply {
                        put("tag", "remote")
                        put("address", "8.8.8.8")
                        put("detour", "proxy")
                    })
                    put(JSONObject().apply {
                        put("tag", "local")
                        put("address", "local")
                    })
                })
            })
            put("experimental", JSONObject().apply {
                put("cache_file", JSONObject().apply {
                    put("enabled", false)
                })
            })
        }
        return config.toString(2)
    }

    private fun parseQuery(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrBlank()) return emptyMap()
        return rawQuery.split("&").mapNotNull { part ->
            val idx = part.indexOf('=')
            if (idx < 0) null
            else URLDecoder.decode(part.substring(0, idx), "UTF-8") to
                    URLDecoder.decode(part.substring(idx + 1), "UTF-8")
        }.toMap()
    }
}
