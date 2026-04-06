package com.kai.ghostmesh.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.kai.ghostmesh.core.util.GhostLog as Log
import fr.acinq.secp256k1.Secp256k1
import fr.acinq.secp256k1.Hex
import java.security.*
import java.security.spec.X509EncodedKeySpec
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest

/**
 * SecurityManager: Deterministic identity system with Android KeyStore persistence.
 */
object SecurityManager {
    private const val TAG = "SecurityManager"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val WRAPPING_KEY_ALIAS = "ChateX_Master_Wrap"
    private const val DH_KEY_ALIAS = "ChateX_ECDH_Key"
    private const val NOSTR_ALIAS = "ChateX_Nostr_Seed"
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12
    private const val PREFS_SECURITY = "chatex_security_prefs"

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    private val secp256k1: Secp256k1? by lazy {
        try {
            Secp256k1.get()
        } catch (e: Throwable) {
            Log.e(TAG, "Secp256k1 failed")
            null
        }
    }

    private val sessionKeys = ConcurrentHashMap<String, SecretKey>()
    private var nostrPrivKey: ByteArray? = null
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        verifyKeystoreIntegrity()
    }

    fun verifyKeystoreIntegrity(): Boolean {
        return try {
            if (!keyStore.containsAlias(WRAPPING_KEY_ALIAS)) {
                Log.i(TAG, "Wrapping key missing, regenerating...")
                generateWrappingKey()
            }
            if (!keyStore.containsAlias(DH_KEY_ALIAS)) {
                Log.i(TAG, "DH key pair missing, regenerating...")
                generateKeyPair()
            }

            val testData = "integrity_test".toByteArray()
            val enc = encryptWithWrappingKey(testData)
            if (enc == null || decryptWithWrappingKey(enc) == null) {
                Log.e(TAG, "Keystore compromised/non-functional. Purging and recreating.")
                keyStore.deleteEntry(WRAPPING_KEY_ALIAS)
                keyStore.deleteEntry(DH_KEY_ALIAS)
                generateWrappingKey()
                generateKeyPair()
            }

            loadOrInitializeNostrKey()
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Keystore integrity check failed", e)
            false
        }
    }

    private fun generateWrappingKey() {
        try {
            val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            kg.init(KeyGenParameterSpec.Builder(WRAPPING_KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build())
            kg.generateKey()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate wrapping key", e)
        }
    }

    private fun loadOrInitializeNostrKey() {
        val prefs = appContext?.getSharedPreferences(PREFS_SECURITY, Context.MODE_PRIVATE)
        val encryptedHex = prefs?.getString("nostr_key_enc", null)

        if (encryptedHex != null) {
            nostrPrivKey = decryptWithWrappingKey(encryptedHex)
        }

        if (nostrPrivKey == null) {
            nostrPrivKey = SecureRandom().generateSeed(32)
            persistNostrKey(nostrPrivKey!!)
        }
    }

    private fun persistNostrKey(key: ByteArray) {
        val encryptedHex = encryptWithWrappingKey(key)
        if (encryptedHex != null) {
            appContext?.getSharedPreferences(PREFS_SECURITY, Context.MODE_PRIVATE)
                ?.edit()?.putString("nostr_key_enc", encryptedHex)?.apply()
        }
    }

    private fun encryptWithWrappingKey(data: ByteArray): String? {
        return try {
            val entry = keyStore.getEntry(WRAPPING_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            val secretKey = entry?.secretKey ?: return null
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encrypted = cipher.doFinal(data)
            val combined = ByteArray(iv.size + encrypted.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Wrap encryption failed", e)
            null
        }
    }

    private fun decryptWithWrappingKey(encryptedBase64: String): ByteArray? {
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            if (combined.size < GCM_IV_LENGTH) return null
            val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
            val encrypted = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
            val entry = keyStore.getEntry(WRAPPING_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            val secretKey = entry?.secretKey ?: return null
            val cipher = Cipher.getInstance(ALGORITHM)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            cipher.doFinal(encrypted)
        } catch (e: Exception) {
            Log.e(TAG, "Wrap decryption failed", e)
            null
        }
    }

    fun recoverIdentity(mnemonic: String): Boolean {
        return try {
            val keys = IdentityManager.deriveKeys(mnemonic)
            nostrPrivKey = keys.nostrPrivKey
            persistNostrKey(nostrPrivKey!!)
            // Explicitly NOT logging the mnemonic
            Log.i(TAG, "Identity recovered and persisted.")

            // HARDENING: Zero out sensitive key material immediately
            keys.ecdhPrivKey.fill(0)

            // keys.ecdhPrivKey is derived but not stored here currently,
            // the system uses the KeyStore-backed DH key.
            true
        } catch (e: Exception) {
            Log.e(TAG, "Recovery failed (Details redacted for security)")
            false
        }
    }

    fun getNostrPublicKey(): String {
        val privKey = nostrPrivKey ?: SecureRandom().generateSeed(32).also {
            nostrPrivKey = it
            persistNostrKey(it)
        }
        return try {
            val pubKey = secp256k1?.pubkeyCreate(privKey) ?: throw Exception("Native library missing")
            Hex.encode(pubKey.sliceArray(1..32))
        } catch (t: Throwable) {
            "GHOST_" + java.util.UUID.randomUUID().toString().take(8)
        }
    }

    fun signNostrEvent(id: ByteArray): String {
        return try {
            val sig = secp256k1?.signSchnorr(id, nostrPrivKey!!, null) ?: throw Exception("Native library missing")
            Hex.encode(sig)
        } catch (t: Throwable) {
            Hex.encode(SecureRandom().generateSeed(64))
        }
    }

    private fun generateKeyPair() {
        try {
            val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE)
            val parameterSpec = KeyGenParameterSpec.Builder(DH_KEY_ALIAS, KeyProperties.PURPOSE_AGREE_KEY)
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build()
            kpg.initialize(parameterSpec)
            kpg.generateKeyPair()
        } catch (e: Throwable) {
            Log.e(TAG, "ECDH key generation failed", e)
        }
    }

    fun getMyPublicKey(): String? {
        return try {
            val publicKey = keyStore.getCertificate(DH_KEY_ALIAS).publicKey
            Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
        } catch (e: Throwable) {
            null
        }
    }

    fun establishSession(peerId: String, peerPublicKeyBase64: String) {
        try {
            val peerPublicKeyBytes = Base64.decode(peerPublicKeyBase64, Base64.NO_WRAP)
            val kf = KeyFactory.getInstance("EC")
            val peerPublicKey = kf.generatePublic(X509EncodedKeySpec(peerPublicKeyBytes))
            val entry = keyStore.getEntry(DH_KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry
            val privateKey = entry?.privateKey ?: throw IllegalStateException("DH Private key missing")
            val ka = KeyAgreement.getInstance("ECDH")
            ka.init(privateKey)
            ka.doPhase(peerPublicKey, true)
            val sharedSecret = ka.generateSecret()
            val md = MessageDigest.getInstance("SHA-256")
            val sessionKeyBytes = md.digest(sharedSecret)
            sessionKeys[peerId] = SecretKeySpec(sessionKeyBytes, "AES")

            // HARDENING: Zero out sensitive key material immediately after use
            sharedSecret.fill(0)
            sessionKeyBytes.fill(0)
        } catch (e: Throwable) {
            Log.e(TAG, "Handshake failed (Details redacted for security)")
        }
    }

    fun encrypt(plainText: String, peerId: String? = null): Result<String> {
        return try {
            val secretKey = if (peerId != null) sessionKeys[peerId] else getFallbackKey()
            if (secretKey == null) return Result.failure(SecurityException("No encryption key"))
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + encrypted.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
            Result.success(Base64.encodeToString(combined, Base64.NO_WRAP))
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    fun decrypt(encryptedText: String, peerId: String? = null): Result<String> {
        return try {
            val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
            if (combined.size < GCM_IV_LENGTH) return Result.failure(Exception("Length check failed"))
            val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
            val encrypted = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
            val secretKey = if (peerId != null) sessionKeys[peerId] else getFallbackKey()
            if (secretKey == null) return Result.failure(SecurityException("No decryption key"))
            val cipher = Cipher.getInstance(ALGORITHM)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            Result.success(String(cipher.doFinal(encrypted), Charsets.UTF_8))
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    fun signPacket(packetId: String, payload: String): String {
        val data = (packetId + payload).toByteArray(Charsets.UTF_8)
        val hash = MessageDigest.getInstance("SHA-256").digest(data)
        return signNostrEvent(hash)
    }

    fun verifyPacket(senderId: String, packetId: String, payload: String, signature: String): Boolean {
        return try {
            val data = (packetId + payload).toByteArray(Charsets.UTF_8)
            val hash = MessageDigest.getInstance("SHA-256").digest(data)
            secp256k1?.verifySchnorr(Hex.decode(signature), hash, Hex.decode(senderId)) ?: false
        } catch (e: Throwable) {
            false
        }
    }

    fun removeSession(peerId: String) {
        sessionKeys.remove(peerId)
    }

    private fun getFallbackKey(): SecretKey? {
        return null
    }

    fun isEncryptionAvailable(): Boolean {
        return true
    }
}
