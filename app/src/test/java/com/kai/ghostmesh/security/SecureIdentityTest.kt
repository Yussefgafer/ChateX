package com.kai.ghostmesh.security

import org.junit.Ignore

import com.kai.ghostmesh.core.security.IdentityManager
import org.junit.Assert.*
import org.junit.Test
import java.util.HashSet

class SecureIdentityTest {

    @Test
    fun testRandomMnemonicGeneration() {
        val count = 10
        val mnemonics = HashSet<String>()
        repeat(count) {
            val m = IdentityManager.generateMnemonic()
            assertEquals("Each mnemonic must have 12 words", 12, m.split(" ").size)
            assertTrue("Mnemonic must be valid: $m", IdentityManager.validateMnemonic(m))
            mnemonics.add(m)
        }
        assertTrue("Mnemonic generation must be random", mnemonics.size > 1)
    }

    @Test
    fun testDeterministicSeedDerivation() {
        val mnemonic = "abandon ability able about above absent absorb abstract absurd abuse access accident"
        val seed1 = IdentityManager.deriveSeed(mnemonic)
        val seed2 = IdentityManager.deriveSeed(mnemonic)
        assertArrayEquals("Seed derivation must be deterministic", seed1, seed2)
        assertEquals("Seed must be 512 bits (64 bytes)", 64, seed1.size)
    }

    @Test
    fun testSubkeyDerivation() {
        val mnemonic = "abandon ability able about above absent absorb abstract absurd abuse access accident"
        val keys1 = IdentityManager.deriveKeys(mnemonic)
        val keys2 = IdentityManager.deriveKeys(mnemonic)
        assertArrayEquals("Nostr key must be deterministic", keys1.nostrPrivKey, keys2.nostrPrivKey)
        assertArrayEquals("ECDH key must be deterministic", keys1.ecdhPrivKey, keys2.ecdhPrivKey)
        assertEquals("Private keys must be 256 bits (32 bytes)", 32, keys1.nostrPrivKey.size)
    }
}
