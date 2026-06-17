package com.safenet.vpn.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import android.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages an EC P-256 key pair stored in the Android Keystore for device attestation.
 *
 * Key properties:
 * - Private key never leaves the Keystore (hardware-backed on supported devices).
 * - Survives app updates, but is deleted on uninstall (Android OS behaviour).
 * - On reinstall, a new key pair is generated. The server detects this via
 *   a changed publicKey for a known fingerprint (ANDROID_ID) and performs
 *   re-attestation, avoiding the old 401 "Invalid device secret" error.
 *
 * Auth flow:
 *   Client builds challenge = "fingerprint:timestamp_minute"
 *   Client signs challenge with private key → ECDSA/SHA-256 signature
 *   Server verifies signature against stored publicKey within ±2 minute window
 */
@Singleton
class DeviceKeyManager @Inject constructor() {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "safenet_device_key_v1"
        private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
    }

    /**
     * Returns the device EC public key as Base64-encoded X.509 DER.
     * Generates the key pair on first call (or after reinstall).
     */
    fun getPublicKeyBase64(): String {
        ensureKeyExists()
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val cert = keyStore.getCertificate(KEY_ALIAS)
        return Base64.encodeToString(cert.publicKey.encoded, Base64.NO_WRAP)
    }

    /**
     * Signs the given challenge string with the device private key.
     * Returns the ECDSA signature as Base64 (No-Wrap, URL-safe compatible).
     */
    fun sign(challenge: String): String {
        ensureKeyExists()
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val privateKeyEntry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.PrivateKeyEntry
        val signer = Signature.getInstance(SIGNATURE_ALGORITHM).apply {
            initSign(privateKeyEntry.privateKey)
            update(challenge.toByteArray(Charsets.UTF_8))
        }
        return Base64.encodeToString(signer.sign(), Base64.NO_WRAP)
    }

    /**
     * Returns true if a Keystore key pair exists (i.e., app was NOT recently reinstalled).
     * Returns false after a fresh install or after uninstall+reinstall.
     */
    fun hasKey(): Boolean {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        return keyStore.containsAlias(KEY_ALIAS)
    }

    /**
     * Builds the challenge string that will be signed.
     * Format: "fingerprint:timestamp_minute"
     * The server accepts ±2 minutes to allow for clock skew.
     */
    fun buildChallenge(fingerprint: String): String {
        val minuteTimestamp = System.currentTimeMillis() / 60_000L
        return "$fingerprint:$minuteTimestamp"
    }

    // ── Private ────────────────────────────────────────────────────────────

    private fun ensureKeyExists() {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
            )
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setUserAuthenticationRequired(false) // no biometric gate — VPN must work silently
                .build()

            KeyPairGenerator
                .getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE_PROVIDER)
                .apply { initialize(spec) }
                .generateKeyPair()
        }
    }
}
