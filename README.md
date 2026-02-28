# 👻 ChateX (GhostMesh) v2.0

[![Spectral Build](https://github.com/Yussefgafer/ChateX/actions/workflows/android.yml/badge.svg)](https://github.com/Yussefgafer/ChateX/actions/workflows/android.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/kotlin-2.3.10-blue.svg)](https://kotlinlang.org)
[![Material 3](https://img.shields.io/badge/Material-3%20Dynamic-purple.svg)](https://m3.material.io)

**ChateX** is a high-performance, decentralized hybrid mesh networking chat application. Designed for total privacy and resilience, it ensures you stay connected even when the world goes dark.
> "No internet? No problem. The mesh is everywhere." 🌌

---

## 🌟 Key Features

### 📡 Hybrid Mesh Core (Non-GMS Compatible)
- **Multi-Transport Support:** Works with **Google Nearby**, **Bluetooth Legacy**, and **LAN/WiFi** simultaneously.
- **LAN Discovery (NSD):** Communicate instantly over local networks using Network Service Discovery.
- **Decentralized Relay:** Every device acts as a spectral relay, extending network range far beyond a single connection.
- **Stealth Mode:** Remain invisible on the radar while still participating in packet relaying.

### 🔐 Spectral Security (Real E2EE)
- **ECDH Key Exchange:** Secure peer-to-peer session keys established via Elliptic Curve Diffie-Hellman.
- **AES-256-GCM Encryption:** Hardware-backed protection via **Android Keystore**.
- **Burn After Reading:** Self-destructing messages with customizable timers.
- **Zero Trust:** No central servers, no logs, no backdoors.

### 🎨 Material 3 Dynamic UI
- **Dynamic Theming:** UI colors adapt perfectly to your device wallpaper (Android 12+).
- **Material 3 Expressive:** Full support for modern Material components and adaptive layouts.
- **Deep Customization:** Control animation speeds, haptic intensity, connection timeouts, and more.
- **90FPS Fluidity:** Hand-tuned graphics layer with `graphics-shapes` for morphing interactions.

### 💬 Rich Messaging
- **Media Support:** Encrypted images and voice notes delivered through the mesh.
- **Smart Replies:** Fluid visual linking to specific messages in the history.
- **Global Shout:** Broadcast messages to all nearby nodes in the void.

---

## 🛠️ Tech Stack

- **Language:** Kotlin 2.3.10 (K2 Compiler)
- **Framework:** Jetpack Compose (Modern Material 3 Dynamic)
- **Architecture:** MVVM + Clean Repository Pattern
- **Persistence:** Room Database (Persistent Migrations)
- **Security:** ECDH + AES-256 (TEE Backed)
- **Networking:** Hybrid (Nearby API + NSD + Bluetooth RFCOMM)

---

## 🚀 Getting Started

1. **Manifest Identity:** Set up your nickname and spectral soul color in Settings.
2. **Scan the Void:** Open the Radar screen to find nearby ghosts.
3. **Establish Link:** Tap a ghost to start a secure, encrypted spectral session.
4. **Customise:** Head to Settings to fine-tune your mesh parameters and visuals.

---

## 📦 Building

### Local Build
```bash
./gradlew assembleDebug
```

### Run Tests
```bash
./gradlew test
```

---
*Developed with 💜 and absolute precision. Inspired by the silence of the void.*
