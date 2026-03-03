# Architecture Analysis: Modular Void

## Overview
The "Modular Void" architecture follows a Clean Architecture approach with a clear separation between `.core` (business logic, mesh, security) and `.features` (UI, ViewModels).

## Strengths
- **Decoupled Transports:** The `MeshTransport` interface and `MultiTransportManager` allow for easy addition of new transport layers (e.g., LoRa, USB).
- **Immutable Models:** Extensive use of `@Immutable` and `data class` ensures efficient Compose recompositions.
- **Repository Pattern:** `GhostRepository` centralizes data access and abstracts Room/Mesh complexities.

## Identified Architectural Risks
1. **Dependency Inversion Violation:**
   - `FileTransferManager` is currently hard-coupled to Google Nearby's `ConnectionsClient`. This breaks the "Modular Void" promise as file transfers won't work over LAN or WiFi Direct without a rewrite.
   - **Recommendation:** Refactor `FileTransferManager` to work with the `MeshTransport` interface or a generic `StreamProvider`.

2. **State Management Fragility:**
   - `MeshManager` uses `MutableSharedFlow` for incoming packets. Under heavy load, if the UI isn't collecting fast enough, packets might be dropped if the buffer (100) overflows.
   - **Recommendation:** Increase buffer capacity or implement a backpressure-aware processing queue in `MeshEngine`.

3. **Lifecycle Management:**
   - `MeshService` is a foreground service, which is correct, but there is no clear "handshake" between `MainActivity` and `MeshService` to ensure the service is fully ready before the UI starts sending commands.
   - **Recommendation:** Implement a `StateFlow` in the Service indicating "Ready" status.

4. **Database Migration Strategy:**
   - The use of `fallbackToDestructiveMigration()` is dangerous for a commercial product. Any schema mismatch will wipe the user's entire chat history.
   - **Recommendation:** Implement robust migration scripts for all future versions.
