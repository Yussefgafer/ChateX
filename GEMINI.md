# 🤖 Gemini/Claude Code Intelligence Guide

This guide provides deep technical context for AI Agents working on the ChateX codebase.

## 📂 Package Structure
- `com.kai.ghostmesh.core.mesh`: The heart of the protocol. `MeshEngine` handles routing, while `MeshManager` coordinates transports.
- `com.kai.ghostmesh.core.mesh.transports`: Plugin implementations for diverse protocols. All must implement `MeshTransport`.
- `com.kai.ghostmesh.core.security`: `SecurityManager` manages the BIP-39 identity, ECDH handshakes, and encryption.
- `com.kai.ghostmesh.features`: Individual UI modules following the MVVM pattern.

## 🧱 Key Singletons (AppContainer)
- `MeshManager`: Orchestrates the entire networking stack.
- `GhostRepository`: Unified data access layer for Room DB and shared prefs.
- `SecurityManager`: Static utility for cryptographic operations.

## 🔄 Protobuf Migration
As of Protocol Version 2, all mesh traffic must use Protobuf (`mesh.proto`).
- DO NOT use GSON for raw mesh packets.
- Ensure `protocolVersion` is checked during the handshake.
- Use uniform padding (1KB) to obfuscate metadata size.

## ⚡ Extension Guidelines
To add a new transport:
1. Implement `MeshTransport`.
2. Handle both `onPacketReceived` (JSON) and `onBinaryPacketReceived` (Protobuf).
3. Register the plugin in `MeshManager.startMesh()`.

## 🔋 Performance Constraints
- **RAM Target:** 84MB (Infinix devices).
- **Disk Streaming:** Always use `RandomAccessFile` for file chunks.
- **Motion:** Use `MaterialTheme.motionScheme` for spring tokens; avoid linear tweens.

---
*Reference Grade Implementation Required.*
