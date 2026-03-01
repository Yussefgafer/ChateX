# 🛡️ ChateX (GhostMesh): The Connected Void

ChateX is a decentralized, serverless communication suite designed for ultimate resilience and privacy. It operates on the **GhostMesh** protocol, a hybrid MANET (Mobile Ad-hoc Network) that bridges local peer-to-peer links with the global Nostr cloud.

## 🌌 Core Pillars
- **GhostMesh Protocol:** Simultaneous multi-transport communication via Bluetooth Legacy, LAN (NSD), and WiFi Direct.
- **The Nostr Bridge:** Hybrid cloud-to-local relaying using Nostr Kind 1/4/10002 events for global reach.
- **Self-Healing Mesh:** Multi-path routing with sub-500ms failover and reputation-based Master election.
- **Spectral Torrent:** Decentralized, chunked file sharing with SHA-256 integrity and a zero-RAM disk-streaming engine.
- **Fidget Physics UI:** A Material 3 Expressive experience with magnetic interaction and spring-based motion schemes.

## 🛠️ Technical Architecture
The project follows a **Modular Void** Clean Architecture:
- `.core`: Routing engine, Protobuf serialization, ECDH/AES-GCM security, and transport plugins.
- `.features`: specialized modules for Discovery, Chat, Torrenting, and Maps.
- `.service`: Foreground lifecycle management and battery-aware scanning.

## 🚀 Build & Deployment
- **Target SDK:** 35 (Android 15)
- **Minimum SDK:** 26
- **Dependencies:** Kotlin 2.x, Jetpack Compose, Protobuf, Ktor CIO, Room Database.
- **Commands:**
    - Build: `./gradlew assembleDebug`
    - Test: `./gradlew testDebugUnitTest`

## 🛡️ Security & Privacy
- **E2EE:** ECDH session establishment with AES-256-GCM encryption.
- **Identity:** BIP-39 deterministic identity with 12-word seed phrases.
- **Metadata Protection:** Uniform Protobuf padding and heartbeat noise traffic.
- **Audit:** Lamport Timestamps for causal message ordering without central clocks.

---
*Architect of the Connected Void.*
