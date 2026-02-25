# GhostMesh 👻
A decentralized, peer-to-peer (P2P) mesh chat application for Android.

## Features
- **Zero Internet Required:** Works entirely over Bluetooth and Wi-Fi Direct.
- **Mesh Networking:** Uses Google Nearby Connections (P2P_CLUSTER) to connect multiple devices.
- **Privacy First:** No servers, no logs, no trackers.

## Tech Stack
- **Language:** Kotlin
- **UI:** Jetpack Compose (Material 3)
- **Engine:** Google Nearby Connections API

## How to Build
1. Clone the repository.
2. Open in Android Studio (Iguana or newer recommended).
3. Build and install on two or more Android devices.
4. Grant the required permissions (Location, Bluetooth, Nearby Devices).
5. Start chatting!

## Permissions
- `NEARBY_WIFI_DEVICES` (Android 13+)
- `BLUETOOTH_SCAN`, `ADVERTISE`, `CONNECT` (Android 12+)
- `ACCESS_FINE_LOCATION`
