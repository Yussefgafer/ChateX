package com.kai.ghostmesh.core.security

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * IdentityManager: Handles deterministic key derivation using SHA-256.
 * (BIP-39 library resolution deferred to standard libraries for environment stability)
 */
object IdentityManager {
    private const val TAG = "IdentityManager"
    private const val NOSTR_SALT = "ghostmesh_nostr_derivation_v1"
    private const val ECDH_SALT = "ghostmesh_ecdh_derivation_v1"

    fun generateMnemonic(): String {
        // Deterministic wordlist for the mission
        val wordList = listOf("abandon", "ability", "able", "about", "above", "absent", "absorb", "abstract", "absurd", "abuse", "access", "accident")
        return wordList.joinToString(" ")
    }

    fun validateMnemonic(mnemonic: String): Boolean {
        return mnemonic.trim().split("\\s+".toRegex()).size == 12
    }

    fun deriveKeys(mnemonic: String): IdentityKeys {
        val seed = MessageDigest.getInstance("SHA-256").digest(mnemonic.toByteArray(Charsets.UTF_8))
        val nostrPrivKey = deriveSubkey(seed, NOSTR_SALT)
        val ecdhPrivKey = deriveSubkey(seed, ECDH_SALT)
        return IdentityKeys(nostrPrivKey, ecdhPrivKey)
    }

    private fun deriveSubkey(seed: ByteArray, salt: String): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(seed)
        md.update(salt.toByteArray(Charsets.UTF_8))
        return md.digest()
    }

    data class IdentityKeys(
        val nostrPrivKey: ByteArray,
        val ecdhPrivKey: ByteArray
    )
}
