package com.kai.ghostmesh.security

import android.util.Base64
import android.util.Log
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object SecurityManager {
    private const val TAG = "SecurityManager"
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private val key = SecretKeySpec("SpectralChateX2026SecureKey32Bit".toByteArray(), "AES")
    private val iv = IvParameterSpec("SpectralIVVector".toByteArray())

    fun encrypt(plainText: String): String {
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, key, iv)
            val encrypted = cipher.doFinal(plainText.toByteArray())
            Base64.encodeToString(encrypted, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed", e)
            plainText
        }
    }

    fun decrypt(encryptedText: String): String {
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, key, iv)
            val decoded = Base64.decode(encryptedText, Base64.DEFAULT)
            String(cipher.doFinal(decoded))
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            encryptedText
        }
    }
}
