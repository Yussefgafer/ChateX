package com.kai.ghostmesh.security

import org.junit.Ignore

import com.kai.ghostmesh.core.security.IdentityManager
import org.junit.Assert.*
import org.junit.Test

class IdentityManagerTest {

    @Test
    fun testMnemonicGeneration() {
        val mnemonic = IdentityManager.generateMnemonic()
        val words = mnemonic.split(" ")
        assertEquals(12, words.size)
        assertTrue(IdentityManager.validateMnemonic(mnemonic))
    }

    @Test
    fun testDeterministicDerivation() {
        val mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"

        val keys1 = IdentityManager.deriveKeys(mnemonic)
        val keys2 = IdentityManager.deriveKeys(mnemonic)

        assertArrayEquals(keys1.nostrPrivKey, keys2.nostrPrivKey)
        assertArrayEquals(keys1.ecdhPrivKey, keys2.ecdhPrivKey)
    }

    @Test
    fun testInvalidMnemonic() {
        assertFalse(IdentityManager.validateMnemonic("invalid words that are not bip39"))
    }
}
