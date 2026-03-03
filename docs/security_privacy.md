# Security & Privacy Analysis

## Current State
- E2EE via ECDH (Handshake implemented).
- Broadcast encryption via daily rotating "Fallback Key".
- BIP-340 Schnorr signatures for Nostr.
- AES-256-GCM for payload encryption.
- **Packet Verification:** `MeshEngine` now verifies signatures using `SecurityManager.verifyPacket`.

## Major Vulnerabilities & Improvements
1. **Auto-Handshake Deficiency:**
   - There is no logic in `MeshEngine` or `ChatViewModel` to automatically trigger `KEY_EXCHANGE` when a new node is discovered. E2EE only works if both parties manually exchange keys, which isn't implemented in the UI.
   - **Recommendation:** Implement an "Auto-Handshake" feature on first contact.

2. **Metadata Leakage:**
   - While the payload is encrypted, the `Packet` header (senderId, receiverId, type) is sent in plain JSON. An observer can map the social graph of the mesh.
   - **Recommendation:** Implement "Envelope" encryption where the inner packet is fully encrypted with a mesh-wide "Network Key".

3. **Identity Fragility:**
   - Nostr keys are derived from a random seed if the Keystore fails. This means users lose their identity if the app's private data is cleared or if the native lib fails to load.
   - **Recommendation:** Implement a 12-word seed phrase (BIP-39) for identity recovery.

4. **Replay Protection:**
   - While `processedPacketIds` prevents immediate loops, it does not prevent a "Delayed Replay Attack" where a captured packet is re-broadcasted 30 minutes later.
   - **Recommendation:** Combine the ID cache with a stricter timestamp window and per-session sequence numbers.
