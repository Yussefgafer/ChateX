# Technical Debt & Code Quality Audit

## Concurrency & Resource Management
1. **Raw Threading:**
   - `FileTransferManager` and some transport layers use raw `Thread { }.start()` or `Executors`. In a modern Android app, these should be migrated to Kotlin Coroutines (`CoroutineScope`) with appropriate `Dispatchers.IO` to ensure better lifecycle integration and cancellation support.
   - **Risk:** Potential thread leaks if the service is stopped while a transfer is active.

2. **Exception Swallowing:**
   - Many networking blocks (e.g., in `LanTransport` and `WifiDirectTransport`) have empty `catch (e: Exception) {}` blocks.
   - **Risk:** Debugging connectivity issues in the field becomes nearly impossible without proper logging or error propagation to the UI.

3. **Deprecated API Usage:**
   - Found several deprecation warnings during build (`fallbackToDestructiveMigration`, `resolveService` for NSD, `centerAlignedTopAppBarColors`).
   - **Risk:** Future Android SDK versions might break these implementations.

## Database & Data Handling
1. **Inefficient Recent Chats Query:**
   - `GhostRepository.recentChats` fetches a large list of messages and groups them in memory using Kotlin's `groupBy`.
   - **Recommendation:** Use a SQL query with `GROUP BY` and `MAX(timestamp)` to let SQLite handle the heavy lifting. This will significantly improve performance as the message history grows.

2. **Missing Database Indices:**
   - The `messages` table lacks an index on `ghostId`.
   - **Risk:** Searching for messages in a specific chat will become a linear scan (`O(N)`), causing UI stutters in long-lived conversations.

3. **Memory Management (LRU Cache):**
   - The `processedPacketIds` cache in `MeshEngine` is fixed at 300. In a very busy mesh network, this might be too small, leading to re-processing of old packets. In a very quiet one, it's fine. It should ideally be time-bound as well as size-bound.

## Code Style & Standards
- **Hardcoded Constants:** Many timeouts and limits are hardcoded in the logic rather than being centralized in `AppConfig` or `Constants`.
- **String Resources:** Excessive use of hardcoded strings in ViewModels and Screens makes internationalization (Arabic support, etc.) difficult.
