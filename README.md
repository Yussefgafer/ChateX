# ChateX

[Build Status](https://github.com/Yussefgafer/ChateX/actions/workflows/android.yml/badge.svg)
[Latest Release](https://github.com/Yussefgafer/ChateX/releases/latest)
[Version](https://github.com/Yussefgafer/ChateX/releases/latest)

**ChateX** is a high-performance, decentralised mesh networking chat application built for the future of private communication. No internet? No problem. 

---

## Features

### Core Features
- **Decentralised Mesh:** Communicates directly between devices using Bluetooth and WiFi Direct.
- **E2EE Encryption:** AES-256-GCM protected spectral packets via Android Keystore.
- **Burn After Reading:** Self-destructing messages for the ultimate privacy.
- **Offline Messaging:** Messages are queued and sent when connection is restored.

### Messaging
- **Message Replies:** Reply to specific messages with visual indicator.
- **Message Status:** Sent, Delivered, and Read receipts.
- **Smart Timestamps:** Human-readable time formatting (now, 5m, 2h, yesterday, etc.).
- **Copy Messages:** Copy text messages to clipboard.
- **Message Search:** Search through conversations.

### File Transfer
- **Chunked File Transfer:** Large files (up to 100MB) are transferred in 32KB chunks.
- **Progress Tracking:** Real-time progress indicators for file transfers.
- **Image Compression:** Automatic compression for images (max 1024px, 500KB).

### Profile & Contacts
- **Profile Pictures:** Support for profile images.
- **Contact Blocking:** Block unwanted contacts.
- **Last Seen:** Track when contacts were last online.
- **Custom Colors:** Choose your spectral color.

### Network
- **Exponential Backoff:** Smart reconnection with exponential backoff.
- **Connection Quality:** Real-time mesh health indicator.
- **Pull-to-Refresh:** Refresh mesh connections easily.

### UI/UX
- **Spectral UI:** A fluid, expressive interface built with **Material 3 Expressive**.
- **90FPS Fluidity:** Hand-tuned graphics layer for maximum smoothness.
- **Dark Theme:** Optimized for night-time spectral communications.
- **Notifications:** Enhanced notifications with message preview.

---

## Tech Stack (2026)

- **Language:** Kotlin 2.3.10 (K2 Compiler)
- **Framework:** Jetpack Compose & Navigation
- **Architecture:** MVVM + Repository Pattern
- **Persistence:** Room Database with JSON Metadata
- **Security:** Android Keystore + AES-256-GCM
- **Networking:** Google Nearby Connections API
- **CI/CD:** GitHub Actions (Auto-Build & Release)

---

## Getting Started

1. Open ChateX on two or more Android devices.
2. Grant permissions (Location, Bluetooth, Notifications).
3. Watch the **Radar** pulse until nodes appear.
4. Tap a node to start a private spectral session.

---

## Building

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

### Tests
```bash
./gradlew test
```

---

## GitHub Actions

The project uses GitHub Actions for CI/CD:
- **Automatic Builds:** Builds on every push to master/main
- **Dual APKs:** Builds both Debug and Release APKs
- **Auto Release:** Creates draft releases on GitHub tags (v*)

### Creating a Release
```bash
git tag v1.0.0
git push origin v1.0.0
```

---

## Permissions Required

- `ACCESS_FINE_LOCATION` - For Bluetooth discovery
- `ACCESS_COARSE_LOCATION` - For Bluetooth discovery
- `BLUETOOTH_SCAN` - Android 12+ Bluetooth scanning
- `BLUETOOTH_ADVERTISE` - Android 12+ Bluetooth advertising
- `BLUETOOTH_CONNECT` - Android 12+ Bluetooth connections
- `NEARBY_WIFI_DEVICES` - Android 13+ WiFi Direct
- `RECORD_AUDIO` - Voice messages
- `POST_NOTIFICATIONS` - Android 13+ notifications

---

## Architecture

```
UI Layer (Compose Screens)
        ↓
ViewModel (GhostViewModel)
        ↓
Repository (GhostRepository)
        ↓
Data Sources (Room DB, MeshService)
        ↓
Network (Nearby Connections API)
```

---

## Security

- All messages are encrypted with **AES-256-GCM**
- Encryption keys are stored in **Android Keystore**
- Keys are hardware-backed and secure
- Self-destructing messages are auto-deleted after reading

---

## License

MIT License - Created with respect by Jo & Kai-Agent.

---

*Communicate freely. Stay spectral.*
