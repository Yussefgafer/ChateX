
<h1 align="center"><font color="#FF0000">IMPORTANT</font></h1>

<p align="center">
🤖 AI-NATIVE PROJECT
This entire codebase, architecture, and UI logic were architected and implemented by LLM (Gemini 3 Flash & Jules[Gemini 3 Flash]) under the strategic direction of Me(Yussef Gafer). No human code was harmed in the making of this mesh.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/AI--GENERATED-100%25-red?style=for-the-badge&logo=openai&logoColor=white" alt="AI Generated">
</p>

<br><br>

---

<p align="center">ChateX - MD3 P2P Chat<p>

<p align="center">
  <img src="https://img.shields.io/github/repo-size/Yussefgafer/ChateX" alt="Repo size">
  <img src="https://github.com/Yussefgafer/ChateX/actions/workflows/android.yml/badge.svg" alt="Build Status">
  <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License">
  <img src="https://img.shields.io/badge/Kotlin-2.3.10-blue.svg" alt="Kotlin">
  <img src="https://img.shields.io/badge/API-21%2B-brightgreen.svg" alt="Min API">
  <img src="https://img.shields.io/badge/Material%20Design-3-purple.svg" alt="Material 3">
  <img src="https://img.shields.io/github/v/release/Yussefgafer/ChateX" alt="Latest release">
  <img src="https://img.shields.io/badge/Gradle-8.2-02303A.svg?logo=gradle" alt="Gradle">
  <img src="https://raw.githubusercontent.com/TheBSD/StandWithPalestine/main/badges/StandWithPalestine.svg" alt="StandWithPalestine">
</p>

ChateX is a professional, high-performance decentralized mesh networking application. It enables private, encrypted communication entirely without internet infrastructure or central servers, utilizing local physical transports and decentralized protocols.

---

## ✨ Key Features

### 📡 Modular Multi-Transport Mesh
ChateX :
- **Google Nearby Connections:** High-bandwidth P2P clustering.
- **Bluetooth Legacy:** Reliable fallback for cross-device compatibility.
- **LAN (NSD):** Seamless communication over local WiFi networks.
- **WiFi Direct:** True P2P connectivity independent of external services.
- **Nostr Bridge:** Decentralized relaying via the Nostr Protocol when internet is available.
- **Multi-path Routing:** Intelligent engine that maintains multiple routes per peer with automatic failover.

### 🎨 Material 3 Discovery Hub
- **Reactive Filtering:** A high-performance `LazyColumn` interface with real-time transport filtering (BT, LAN, WiFi-D, Nostr).
- **Jelly Physics:** Tactile UI interactions using non-uniform scaling and spring physics for an organic feel.
- **Burn Protocol:** Physical message self-destruction using GPU-accelerated fragment shaders (Liquid Erosion effect).
- **Performance Optimized:** Designed for low-resource devices with a strict **84MB RAM** heap constraint and **60FPS** target.

### 🔐 Mesh Security & Identity
- **End-to-End Encryption:** AES-256-GCM protected packets via Android Keystore.
- **Hardware-backed ECDH:** Secure P2P session key exchange.
- **BIP-340 Schnorr Signatures:** Cryptographic packet signing to prevent impersonation.
- **BIP-39 Identity Portability:** 12-word seed phrase system for deterministic account recovery and key derivation.

---

## 🏗️ Architecture

ChateX follows a clean, layered architecture optimized for the **Modular Network** paradigm:

### 📦 Core Layer (`.core`)
- **`.mesh`:** Routing engine, packet deduplication, and transport plugin system.
- **`.security`:** Key management (Keystore/ECDH), Schnorr signatures, and BIP-39 logic.
- **`.ui`:** Shared physics modifiers, MD3 theme, and shader-based components.
- **`.data`:** Room Database with optimized SQL migrations and repository patterns.
- **`.model`:** `@Immutable` data structures for efficient Compose recompositions.

### 🖼️ Feature Layer (`.features`)
- **`.messages`:** Central hub for active mesh conversations.
- **`.chat`:** Real-time E2EE messaging with file streaming and "Burn" support.
- **`.discovery`:** Reactive peer discovery list with telemetry overlays.
- **`.settings`:** Advanced configuration suite for network and UI tuning.

---

## 🚀 Quick Start

### Build from Source
1. **Clone the repository:**
   ```bash
   git clone https://github.com/Yussefgafer/ChateX.git
   cd ChateX
   ```
2. **Open in Android Studio** (Ladybug or newer recommended).
3. **Build the project:**
   ```bash
   ./gradlew clean assembleDebug
   ```
4. **Run Stress Tests:**
   ```bash
   ./gradlew testDebugUnitTest
   ```

---

## 📄 License
Distributed under the MIT License. See `LICENSE` for more information.

---

## 🙏 Acknowledgements
- **Material Design 3** for the expressive UI framework.
- **Nostr Protocol** for decentralized identity and relaying inspiration.
- **Kotlin Coroutines** for high-concurrency mesh processing.