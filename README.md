# 👻 ChateX (GhostMesh) - Decentralized Material 3 Mesh

[![Spectral Build](https://github.com/Yussefgafer/ChateX/actions/workflows/android.yml/badge.svg)](https://github.com/Yussefgafer/ChateX/actions/workflows/android.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/kotlin-2.3.10-blue.svg)](https://kotlinlang.org)
[![Material 3](https://img.shields.io/badge/Material-3%20Expressive-purple.svg)](https://m3.material.io)

**ChateX** is a high-performance, professional decentralized mesh networking chat application. Built for the future of private communication, it operates entirely without the internet or central servers.

> "No internet? No problem. The void is always open." 🌌

---

## 🌟 Key Features

### 📡 Modular Multi-Transport Mesh (Plugin Architecture)
ChateX uses a **Decoupled Plugin Architecture** for its transport layer, allowing multiple connection methods to run concurrently:
- **Google Nearby Connections:** High-bandwidth P2P clustering.
- **Bluetooth Legacy:** Reliable fallback for all devices.
- **LAN (NSD):** Seamless communication over local WiFi networks.
- **WiFi Direct:** Peer-to-peer connectivity independent of Google Play Services.
- **Cloud Nostr Bridge:** Decentralized relaying via Nostr Protocol when internet is available.
- **Multi-hop Routing:** Intelligent routing engine with path cost calculation (battery/latency).

### 🎨 Material 3 Expressive UI (The Void Aesthetic)
- **Fidget Physics Engine:** Tactile UI with organic inertia, magnetic snapping (`magneticClickable`), and 3D leaning (`physicalTilt`).
- **God Mode Customization:** Full control over UI parameters (Corner Radius, Font Scaling) and Network tuning (Timeouts, Cache sizes).
- **Professional Radar:** A minimalist, pulsing interface to discover nearby nodes in the void.

### 🔐 Spectral Security
- **E2EE Encryption:** AES-256-GCM protected spectral packets via Android Keystore.
- **Hardware-backed ECDH:** Secure peer-to-peer session key exchange.
- **BIP-340 Schnorr:** Cryptographically signed Nostr events for the Cloud Bridge.
- **Stealth Mode:** Stay invisible on the radar while still receiving packets from the void.

---

## 🏗️ Architecture: The Modular Void

ChateX follows a **Clean Modular Architecture** split into specialized layers:

### 📦 Core Layer (`.core`)
- **`.mesh`**: The Spectral Routing engine, packet deduplication, and the Transport Plugin system.
- **`.security`**: Encryption, Key Management (Keystore/ECDH), and Schnorr signatures.
- **`.ui`**: Shared Fidget Physics modifiers, MD3E Theme, and Atomic components.
- **`.data`**: Room Database, DAOs, and the centralized Repository.
- **`.model`**: @Immutable data structures for optimized 90FPS performance.

### 🖼️ Feature Layer (`.features`)
- **`.messages`**: Hub for recent spectral conversations.
- **`.chat`**: Real-time E2EE messaging with typing indicators.
- **`.discovery`**: Tactile Radar visualization for node discovery.
- **`.settings`**: God Mode configuration and profile manifestation.

---

## 🧪 Quality Assurance

ChateX is covered by a comprehensive **Unit Test Suite** (100% Pass) using MockK and Coroutines Test:
- **Mesh Integrity**: Routing algorithms, packet deduplication, and stress testing.
- **Data Layer**: Repository mapping and DAO interaction verification.
- **ViewModels**: Screen state logic and flow reactivity.

---

## 🚀 Building

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

---
*Created with 💜 and Surgical Refactoring by Jules. Inspired by the silence of the void.*
