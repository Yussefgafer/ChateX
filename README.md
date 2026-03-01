# ChateX  - Decentralized Material 3 Mesh

<p align="center">
  <img src="https://img.shields.io/github/repo-size/Yussefgafer/ChateX" alt="Repo size">
  <img src="https://github.com/Yussefgafer/ChateX/actions/workflows/android.yml/badge.svg" alt="Build">
  <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License">
  <img src="https://img.shields.io/badge/Kotlin-2.3.10-blue.svg" alt="Kotlin">
  <img src="https://img.shields.io/badge/API-21%2B-brightgreen.svg" alt="Min API">
  <img src="https://img.shields.io/badge/Material%20Design-3%20Expressive-purple.svg" alt="Material 3">
  <img src="https://img.shields.io/github/v/release/Yussefgafer/ChateX" alt="Latest release">
  <img src="https://img.shields.io/badge/Gradle-8.2-02303A.svg?logo=gradle" alt="Gradle">
  <img src="https://raw.githubusercontent.com/TheBSD/StandWithPalestine/main/badges/StandWithPalestine.svg" alt="StandWithPalestine">
</p>

ChateX is a high-performance(Maybe 🙃), professional(Maybe 🙃) decentralized mesh networking chat application. Built for the future of private communication, it operates entirely without the internet or central servers. Connect with others in the void—no infrastructure required.

"No internet? No problem. The void is always open." 🌌

---

✨ Key Features

📡 Modular Multi-Transport Mesh (Plugin Architecture)

ChateX uses a Decoupled Plugin Architecture that allows multiple connection methods to run concurrently, ensuring maximum compatibility and reliability:

· Google Nearby Connections – High-bandwidth P2P clustering (requires Google Play Services).
· Bluetooth Legacy – Reliable fallback for all devices.
· LAN (NSD) – Seamless communication over local WiFi networks.
· WiFi Direct – True peer-to-peer connectivity independent of Google Play Services.
· Cloud Nostr Bridge – Decentralized relaying via the Nostr Protocol when internet is available.
· Multi-hop Routing – Intelligent routing engine with path cost calculation (battery/latency) to optimize message delivery.

🎨 Material 3 Expressive UI

· Fidget Physics Engine – Tactile UI with organic inertia, magnetic snapping (magneticClickable), and 3D leaning (physicalTilt).
· Deep Customization – Full control over UI parameters (corner radius, font scaling) and network tuning (timeouts, cache sizes).
· Professional Radar – A minimalist, pulsing interface to discover nearby nodes in the void. Toggle visibility to stay hidden.

🔐 Spectral Security

· E2EE Encryption – AES-256-GCM protected spectral packets via Android Keystore.
· Hardware-backed ECDH – Secure peer-to-peer session key exchange.
· BIP-340 Schnorr Signatures – Cryptographically signed Nostr events for the Cloud Bridge.
· Stealth Mode – Become invisible on the radar while still receiving packets from the void.

---


📱 System Requirements

· Android 5.0 (API 21) or higher.
· For Nearby Connections & WiFi Direct: devices with Google Play Services and appropriate hardware.
· Bluetooth Low Energy (BLE) support optional but recommended.

---

🚀 Quick Start

Build from Source

1. Clone the repository:
   ```bash
   git clone https://github.com/Yussefgafer/ChateX.git
   cd ChateX
   ```
2. Open the project in Android Studio (latest version recommended).
3. Build the project:
   ```bash
   ./gradlew clean assembleDebug
   ```
4. Run tests (optional):
   ```bash
   ./gradlew testDebugUnitTest
   ```

---

🏗️ Architecture: The Modular Void

ChateX follows a Clean Modular(Maybe 😗) Architecture split into specialized layers to ensure scalability and maintainability.

📦 Core Layer (.core)

· .mesh – The Spectral Routing engine, packet deduplication, and the Transport Plugin system.
· .security – Encryption, Key Management (Keystore/ECDH), and Schnorr signatures.
· .ui – Shared Fidget Physics modifiers, MD3E Theme, and Atomic components.
· .data – Room Database, DAOs, and the centralized Repository.
· .model – @Immutable data structures for optimized 90FPS performance.

🖼️ Feature Layer (.features)

· .messages – Hub for recent spectral conversations.
· .chat – Real-time E2EE messaging with typing indicators.
· .discovery – Tactile Radar visualization for node discovery.
· .settings – God Mode configuration and profile manifestation.


---

📄 License

Distributed under the MIT License. See LICENSE for more information.

---

🙏 Acknowledgements

· Built with 💜 and Jules (Gemini 3 Flash)
· Material Design 3 for the expressive UI guidelines
· Nostr Protocol for decentralized relay inspiration