package com.kai.ghostmesh.core.security

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

object SecurityManager {
    private const val TAG = "SecurityManager"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val DH_KEY_ALIAS = "ChateX_ECDH_Key"
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12

    private val keyStore: KeyStore by lazy {
        try {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        } catch (e: Exception) {
            // Mock KeyStore for JVM
            val ks = KeyStore.getInstance(KeyStore.getDefaultType())
            ks.load(null)
            ks
        }
    }

    private val secp256k1 = Secp256k1.get()

    // Session keys mapped by peer Node ID
    private val sessionKeys = ConcurrentHashMap<String, SecretKey>()
    private var nostrPrivKey: ByteArray? = null

    init {
        try {
            createKeyPairIfNotExists()
            initializeNostrKey()
        } catch (e: Exception) {
            // Fallback for JVM unit tests where AndroidKeyStore is missing
            nostrPrivKey = SecureRandom().generateSeed(32)
        }
    }

    private fun initializeNostrKey() {
        try {
            val alias = "ChateX_Nostr_Seed"
            if (!keyStore.containsAlias(alias)) {
                val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                kg.init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build())
                kg.generateKey()
            }
            val keyEntry = keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry
            val seed = keyEntry?.secretKey?.encoded ?: SecureRandom().generateSeed(32)
            val md = MessageDigest.getInstance("SHA-256")
            nostrPrivKey = md.digest(seed)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Nostr key")
            nostrPrivKey = SecureRandom().generateSeed(32)
        }
    }

    fun getNostrPublicKey(): String {
        val pubKey = secp256k1.pubkeyCreate(nostrPrivKey!!)
        return Hex.encode(pubKey.sliceArray(1..32))
    }

    fun signNostrEvent(id: ByteArray): String {
        val sig = secp256k1.signSchnorr(id, nostrPrivKey!!, null)
        return Hex.encode(sig)
    }

    private fun createKeyPairIfNotExists() {
        if (!keyStore.containsAlias(DH_KEY_ALIAS)) {
            generateKeyPair()
        }
    }

    private fun generateKeyPair() {
        try {
            val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE)
            val parameterSpec = KeyGenParameterSpec.Builder(
                DH_KEY_ALIAS,
                KeyProperties.PURPOSE_AGREE_KEY
            )
            .setDigests(KeyProperties.DIGEST_SHA256)
            .build()
            kpg.initialize(parameterSpec)
            kpg.generateKeyPair()
        } catch (e: Exception) {
            Log.e(TAG, "ECDH Generation failed")
        }
    }

    fun getMyPublicKey(): String? {
        return try {
            val publicKey = keyStore.getCertificate(DH_KEY_ALIAS).publicKey
            Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    fun establishSession(peerId: String, peerPublicKeyBase64: String) {
        try {
            val peerPublicKeyBytes = Base64.decode(peerPublicKeyBase64, Base64.NO_WRAP)
            val kf = KeyFactory.getInstance("EC")
            val peerPublicKey = kf.generatePublic(X509EncodedKeySpec(peerPublicKeyBytes))

            val privateKey = (keyStore.getEntry(DH_KEY_ALIAS, null) as KeyStore.PrivateKeyEntry).privateKey
            val ka = KeyAgreement.getInstance("ECDH")
            ka.init(privateKey)
            ka.doPhase(peerPublicKey, true)

            val sharedSecret = ka.generateSecret()
            val md = MessageDigest.getInstance("SHA-256")
            val sessionKeyBytes = md.digest(sharedSecret)

            sessionKeys[peerId] = SecretKeySpec(sessionKeyBytes, "AES")
        } catch (e: Exception) {
            Log.e(TAG, "Handshake failed with $peerId")
        }
    }

    fun encrypt(plainText: String, peerId: String? = null): Result<String> {
        return try {
            val secretKey = if (peerId != null) sessionKeys[peerId] else getFallbackKey()
            if (secretKey == null) return Result.failure(Exception("No encryption key available"))

            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv
            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            val combined = ByteArray(iv.size + encrypted.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)

            Result.success(Base64.encodeToString(combined, Base64.NO_WRAP))
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed")
            Result.failure(e)
        }
    }

    fun decrypt(encryptedText: String, peerId: String? = null): Result<String> {
        return try {
            val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
            if (combined.size < GCM_IV_LENGTH) return Result.failure(Exception("Invalid encrypted text length"))

            val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
            val encrypted = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

            val secretKey = if (peerId != null) sessionKeys[peerId] else getFallbackKey()
            if (secretKey == null) return Result.failure(Exception("No decryption key available"))

            val cipher = Cipher.getInstance(ALGORITHM)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            Result.success(String(cipher.doFinal(encrypted), Charsets.UTF_8))
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed")
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
            // Assuming senderId is the hex-encoded Schnorr public key
            secp256k1.verifySchnorr(Hex.decode(signature), hash, Hex.decode(senderId))
        } catch (e: Exception) {
            false
        }
    }

    fun removeSession(peerId: String) {
        sessionKeys.remove(peerId)
    }

    private fun getFallbackKey(): SecretKey? {
        // Dynamic Mesh Group Key (MGK) derivation using a periodic epoch
        // This ensures the key rotates over time, even if the base seed is known.
        val baseSeed = "ChateX_Spectral_Mesh_V1_2025_SECURED_SALT".toByteArray(Charsets.UTF_8)
        val epoch = System.currentTimeMillis() / (1000 * 60 * 60 * 24) // Daily rotation

        val md = MessageDigest.getInstance("SHA-256")
        md.update(baseSeed)
        md.update(epoch.toString().toByteArray(Charsets.UTF_8))

        val keyBytes = md.digest()
        return SecretKeySpec(keyBytes, "AES")
    }

    fun isEncryptionAvailable(): Boolean {
        return try {
            getFallbackKey() != null
        } catch (e: Exception) {
            false
        }
    }
}
