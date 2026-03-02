# ChateX Code Audit Report

**Date:** 2026-03-02
**Auditor:** Jules (Senior Software Engineer)
**Scope:** Networking, Security, UI/UX, Data, Performance

---

## 📡 Networking

### 1. Thread-Unsafe Packet Deduplication
- **File Path:** `app/src/main/java/com/kai/ghostmesh/core/mesh/MeshEngine.kt` (Lines 31-37)
- **Issue:** Uses `Collections.newSetFromMap` with a non-thread-safe `LinkedHashMap` for packet deduplication.
- **Impact:** **HIGH.** Since multiple transport threads call `processIncomingJson` concurrently, the internal state of the `LinkedHashMap` can become corrupted, leading to crashes or missed packet deduplication.
- **Suggestion:** Wrap the set in `Collections.synchronizedSet()` or use a concurrent-friendly cache implementation.
- **Priority:** HIGH

### 2. Race Condition in Packet Statistics
- **File Path:** `app/src/main/java/com/kai/ghostmesh/core/mesh/MeshManager.kt` (Lines 76, 88)
- **Issue:** Performs non-atomic increment (`value++`) on `MutableStateFlow` for packet counts from multiple threads.
- **Impact:** **MEDIUM.** Statistics for total packets sent/received will be inaccurate under high load due to lost updates.
- **Suggestion:** Use `java.util.concurrent.atomic.AtomicInteger` or `update { it + 1 }` on the StateFlow to ensure atomicity.
- **Priority:** MEDIUM

### 3. Redundant Nostr Relay Subscriptions
- **File Path:** `app/src/main/java/com/kai/ghostmesh/core/mesh/transports/CloudTransport.kt` (Lines 43-48)
- **Issue:** Connects to all configured relays and subscribes to the same filters on all of them simultaneously.
- **Impact:** High bandwidth usage and redundant packet reception (N times for N relays). The `MeshEngine` must work harder to deduplicate these identical packets.
- **Suggestion:** Implement an adaptive relay selection strategy. Subscribe to one primary relay and only failover/multi-subscribe if latency is high or connection is lost.
- **Priority:** MEDIUM

### 4. File Transfer OOM Risk
- **File Path:** `app/src/main/java/com/kai/ghostmesh/core/mesh/FileTransferManager.kt` (Lines 89-99)
- **Issue:** `splitFileIntoChunks` reads the entire file into a `MutableList<ByteArray>` in memory.
- **Impact:** **CRITICAL.** Attempting to send a 100MB file on a device with an 84MB RAM target will cause an immediate `OutOfMemoryError` and crash the application.
- **Suggestion:** Refactor to read and send chunks sequentially from a `FileInputStream` without loading the entire file into a list.
- **Priority:** CRITICAL

---

## 🔐 Security

### 1. Hardcoded Broadcast Encryption Key
- **File Path:** `app/src/main/java/com/kai/ghostmesh/core/security/SecurityManager.kt` (Lines 159-164)
- **Issue:** Uses a static hardcoded string (`ChateX_Spectral_Mesh_V1_2025`) to derive the AES key for "Shout" messages.
- **Impact:** **CRITICAL.** Any user with the application binary can decrypt every global broadcast message. This is security by obscurity.
- **Suggestion:** Implement a Mesh Group Key (MGK) protocol where the current "Master" node rotates a shared secret encrypted for all known members via their ECDH keys.
- **Priority:** CRITICAL

### 2. Unsafe Encryption/Decryption Fallback
- **File Path:** `app/src/main/java/com/kai/ghostmesh/core/security/SecurityManager.kt` (Lines 127, 154)
- **Issue:** Returns the original plain/cipher text if an encryption/decryption error occurs.
- **Impact:** Users might unknowingly send sensitive messages in plain text if the key agreement failed, providing a false sense of security.
- **Suggestion:** Return a `Result<String>` or throw a custom `SecurityException`. Ensure the UI prevents sending if encryption fails.
- **Priority:** HIGH

### 3. Missing Packet Authentication for Local Mesh
- **File Path:** `app/src/main/java/com/kai/ghostmesh/core/model/Models.kt`
- **Issue:** `Packet` data class lacks a signature field.
- **Impact:** While direct messages are E2EE via ECDH, broadcast messages (Shouts) and packet headers can be easily spoofed by any node on the local mesh. A malicious node could impersonate any `senderId`.
- **Suggestion:** Include a Schnorr signature in every packet, verified against the sender's public key (which is their Node ID).
- **Priority:** HIGH

---

## 🎨 UI/UX

### 1. Expensive Calculations in RadarView Recomposition
- **File Path:** `app/src/main/java/com/kai/ghostmesh/core/ui/components/RadarView.kt` (Lines 117-118, 149-150)
- **Issue:** Performs `cos`, `sin`, and coordinate math directly in the Composable scope during every frame of animation.
- **Impact:** Causes UI jank (frame drops) when many nodes are visible, as the CPU struggles to keep up with 60/90 FPS while recalculating constant offsets.
- **Suggestion:** Use `remember(node.id)` to cache base positions or use lambda-based `Modifier.offset { IntOffset(...) }` to move calculation to the layout phase.
- **Priority:** HIGH

### 2. Lack of In-App Knowledge Base implementation
- **File Path:** `app/src/main/java/com/kai/ghostmesh/features/docs/DocsScreen.kt` (Missing)
- **Issue:** Memory mentions an "offline help system using a WebView-based DocsScreen", but no such implementation exists in the current codebase.
- **Impact:** Users lack guidance on complex mesh and physics concepts within the app.
- **Suggestion:** Implement the `DocsScreen` as described in the technical directives.
- **Priority:** LOW

---

## 💾 Data

### 1. In-Memory Metadata Filtering for Self-Destruct
- **File Path:** `app/src/main/java/com/kai/ghostmesh/core/data/repository/GhostRepository.kt` (Lines 173-181)
- **Issue:** Fetches *all* messages containing self-destruct markers from the DB and filters them by time in Kotlin.
- **Impact:** High memory overhead and slow performance as the database grows.
- **Suggestion:** Add an `expiry_timestamp` column to `MessageEntity` and perform a single `DELETE` query in `MessageDao`.
- **Priority:** HIGH

### 2. Redundant JSON Parsing in Repository
- **File Path:** `app/src/main/java/com/kai/ghostmesh/core/data/repository/GhostRepository.kt` (Lines 20-42)
- **Issue:** Parses the metadata JSON string for every message every time the Flow emits.
- **Impact:** Significant CPU/RAM pressure for large chat histories, even with the small `metaCache`.
- **Suggestion:** Use Room @Embedded or separate columns for common metadata like `isImage`, `isVoice`, and `hops`.
- **Priority:** MEDIUM

---

## ⚡ Performance

### 1. LogBuffer Memory Usage
- **File Path:** `app/src/main/java/com/kai/ghostmesh/core/util/LogBuffer.kt` (Line 7)
- **Issue:** Uses a `LinkedList<String>` to store the last 500 logs.
- **Impact:** Each `String` and `Node` object adds overhead. On an 84MB RAM target, 500 large log strings could consume a non-trivial percentage of the heap.
- **Suggestion:** Use a fixed-size Circular Buffer (`ArrayDeque`) and consider storing log levels as integers to save space.
- **Priority:** LOW

### 2. MeshManager Singleton Initialization
- **File Path:** `app/src/main/java/com/kai/ghostmesh/core/mesh/MeshManager.kt`
- **Issue:** Initializes all transport plugins (`GoogleNearby`, `Bluetooth`, `WifiDirect`, `LAN`) regardless of whether they are needed or enabled.
- **Impact:** High initial memory footprint and potentially starting unused hardware radio scans.
- **Suggestion:** Implement lazy-loading for transports, only initializing them when the user explicitly enables them in settings.
- **Priority:** MEDIUM
