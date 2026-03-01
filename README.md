# 🛡️ ChateX (GhostMesh): The Connected Void

<p align="center">
  <img src="https://img.shields.io/github/repo-size/Yussefgafer/ChateX" alt="Repo size">
  <img src="https://github.com/Yussefgafer/ChateX/actions/workflows/android.yml/badge.svg" alt="Build">
  <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License">
  <img src="https://img.shields.io/badge/Kotlin-2.x-blue.svg" alt="Kotlin">
  <img src="https://img.shields.io/badge/API-26%2B-brightgreen.svg" alt="Min API">
  <img src="https://img.shields.io/badge/Material%20Design-3%20Expressive-purple.svg" alt="Material 3">
  <img src="https://img.shields.io/github/v/release/Yussefgafer/ChateX" alt="Latest release">
  <img src="https://raw.githubusercontent.com/TheBSD/StandWithPalestine/main/badges/StandWithPalestine.svg" alt="StandWithPalestine">
</p>

ChateX is a decentralized, serverless communication suite designed for ultimate resilience and privacy. It operates on the **GhostMesh** protocol, a hybrid MANET (Mobile Ad-hoc Network) that bridges local peer-to-peer links with the global Nostr cloud.

> "No internet? No problem. The void is always open." 🌌

---

## 🌌 Core Pillars
- **GhostMesh Protocol:** Simultaneous multi-transport communication via Bluetooth Legacy, LAN (NSD), and WiFi Direct.
- **The Nostr Bridge:** Hybrid cloud-to-local relaying using Nostr Kind 1/4/10002 events for global reach.
- **Self-Healing Mesh:** Multi-path routing with sub-500ms failover and reputation-based Master election.
- **Spectral Torrent:** Decentralized, chunked file sharing with SHA-256 integrity and a zero-RAM disk-streaming engine.
- **Fidget Physics UI:** A Material 3 Expressive experience with magnetic interaction and spring-based motion schemes.

---

## 🛠️ Technical Architecture
The project follows a **Modular Void** Clean Architecture:
- **`.core`**: Routing engine, Protobuf serialization, ECDH/AES-GCM security, and transport plugins.
- **`.features`**: Specialized modules for Discovery, Chat, Torrenting, and Maps.
- **`.service`**: Foreground lifecycle management and battery-aware scanning.

---

## 🚀 Quick Start
1. **Clone the repository:**
   ```bash
   git clone https://github.com/Yussefgafer/ChateX.git
   cd ChateX
   ```
2. **Build the project:**
   ```bash
   ./gradlew clean assembleDebug
   ```
3. **Run tests:**
   ```bash
   ./gradlew testDebugUnitTest
   ```

---

## 🛡️ Security & Privacy
- **E2EE:** ECDH session establishment with AES-256-GCM encryption.
- **Identity:** BIP-39 deterministic identity with 12-word seed phrases.
- **Metadata Protection:** Uniform Protobuf padding and heartbeat noise traffic.
- **Audit:** Lamport Timestamps for causal message ordering without central clocks.

---

## 📄 License
Distributed under the MIT License. See `LICENSE` for more information.

---

*Architect of the Connected Void.*
