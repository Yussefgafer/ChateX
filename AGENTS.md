# AGENTS.md - ChateX Development Guide

This file provides guidance for AI agents working on the ChateX codebase.

---

## Project Overview

**ChateX** is a decentralized mesh networking chat application built with:
- **Language:** Kotlin 2.3.10 (K2 Compiler)
- **UI Framework:** Jetpack Compose with Material 3 Expressive
- **Architecture:** MVVM + Repository Pattern
- **Database:** Room Database
- **Networking:** Google Nearby Connections API
- **Build System:** Gradle (Kotlin DSL)

---

## Build Commands

### Basic Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean
```

### Running Tests

```bash
# Run all unit tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.kai.ghostmesh.mesh.MeshEngineTest"

# Run a specific test method
./gradlew test --tests "com.kai.ghostmesh.mesh.MeshEngineTest.test packet relay from A to C through B"

# Run Android instrumented tests
./gradlew connectedAndroidTest

# Run with debug output
./gradlew test --info
```

### Linting

```bash
# Run Android lint
./gradlew lint

# Run lint for a specific variant
./gradlew lintDebug
```

---

## Code Style Guidelines

### General Principles

- **Concise Code:** Prefer single-line function bodies where logical
- **No Comments:** Do not add comments unless explicitly required by the user
- **Explicit Typing:** Always declare explicit types for public properties and function return types

### Naming Conventions

| Element | Convention | Example |
|---------|------------|---------|
| Classes | PascalCase | `MeshEngine`, `GhostViewModel` |
| Functions | camelCase | `sendMessage()`, `processIncomingJson()` |
| Properties | camelCase | `myNodeId`, `onlineGhosts` |
| Constants | UPPER_SNAKE_CASE | `MAX_PROCESSED_PACKETS` |
| Package Names | lowercase | `com.kai.ghostmesh.mesh` |
| Enum Values | PascalCase | `PacketType.CHAT`, `MessageStatus.SENT` |

### File Organization

```
app/src/main/java/com/kai/ghostmesh/
├── MainActivity.kt
├── model/           # Data classes (Models.kt, Constants.kt, RecentChat.kt)
├── data/
│   ├── local/       # Room database (Entities.kt, MessageDao.kt, ProfileDao.kt, AppDatabase.kt)
│   └── repository/  # Repository layer (GhostRepository.kt)
├── mesh/            # Mesh networking (MeshEngine.kt, MeshManager.kt)
├── security/        # Encryption (SecurityManager.kt)
├── service/         # Background services (MeshService.kt)
└── ui/
    ├── theme/       # Compose theming (Theme.kt, Color.kt, Type.kt)
    ├── components/  # Reusable UI components
    ├── GhostViewModel.kt
    ├── ChatScreen.kt
    ├── RadarScreen.kt
    └── ...
```

### Import Style

- Group imports in this order:
  1. Android framework imports (`android.*`)
  2. Kotlin standard library (`kotlin.*`)
  3. Third-party libraries (`com.google.*`, `io.mockk.*`, etc.)
  4. Project imports (`com.kai.ghostmesh.*`)

- Use wildcard imports sparingly; prefer specific imports for clarity

### Compose Guidelines

- Use `@Composable` annotation for all composable functions
- Place `@OptIn` annotations before `@Composable` on the same line or separate line above
- Use `MaterialTheme` colors and typography for consistency
- Avoid hardcoded colors; use the theme or define in `ui/theme/Color.kt`
- Use `Modifier` chaining for layout composition

### Data Classes

```kotlin
// Preferred: Explicit types, default values
data class Packet(
    val id: String = UUID.randomUUID().toString(),
    val senderId: String,
    val senderName: String,
    val receiverId: String = "ALL",
    val type: PacketType,
    val payload: String,
    val hopCount: Int = 3,
    val isSelfDestruct: Boolean = false,
    val expirySeconds: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
```

### Error Handling

- Use `try-catch` blocks for operations that may fail
- Return `null` or default values for recoverable errors
- Use `?.let` and Elvis operator (`?:`) for null safety
- Log errors with `e.printStackTrace()` for debugging

```kotlin
// Good patterns
val packet = try {
    gson.fromJson(json, Packet::class.java)
} catch (e: Exception) { return } ?: return

private fun fileToBase64(file: File): String? = 
    try { Base64.encodeToString(file.readBytes(), Base64.DEFAULT) } catch (e: Exception) { null }
```

### State Management

- Use `MutableStateFlow` for mutable state
- Expose immutable `StateFlow` to UI
- Use `stateIn()` with `SharingStarted.Lazily` for derived state
- Avoid `var` properties; prefer `MutableStateFlow`

```kotlin
// ViewModel pattern
private val _onlineGhosts = MutableStateFlow<Map<String, UserProfile>>(emptyMap())
val onlineGhosts = _onlineGhosts.asStateFlow()
```

### Testing Guidelines

- Use JUnit 4 with MockK for mocking
- Use descriptive test names with backticks for spaces
- Follow Arrange-Act-Assert structure
- Test one thing per test method

```kotlin
@Test
fun `test packet relay from A to C through B`() {
    // Arrange
    val relayedPackets = mutableListOf<Packet>()
    val engine = MeshEngine(...)

    // Act
    engine.processIncomingJson("ENDPOINT_A", jsonFromA)

    // Assert
    assertEquals("B should relay 1 packet", 1, relayedPackets.size)
}
```

---

## Key Architecture Patterns

### MVVM + Repository

```
UI Layer (Compose Screens)
        ↓
ViewModel (GhostViewModel)
        ↓
Repository (GhostRepository)
        ↓
Data Sources (Room DB, MeshService)
```

### Packet Flow

1. User sends message → `GhostViewModel.sendMessage()`
2. Creates `Packet` with encrypted payload
3. `MeshService.sendPacket()` transmits via Nearby Connections
4. Receiving devices process via `MeshEngine.processIncomingJson()`
5. `MeshEngine` handles or relays based on `receiverId` and `hopCount`
6. Messages saved to Room via `GhostRepository`

---

## Common Tasks

### Adding a New Screen

1. Create composable in `ui/` directory
2. Add navigation route in `MainActivity.kt`
3. Add ViewModel state collection if needed

### Adding a New Data Model

1. Add data class in `model/Models.kt`
2. Create Room Entity in `data/local/Entities.kt` if persistence needed
3. Add DAO method in appropriate DAO file
4. Update Repository if needed

### Modifying Mesh Protocol

1. Add new `PacketType` enum value in `model/Models.kt`
2. Update `MeshEngine.processIncomingJson()` handling
3. Update `GhostViewModel.handleIncomingPacket()` if UI response needed

---

## CI/CD

GitHub Actions builds on every push to main/master:
- JDK 21
- Runs `./gradlew assembleDebug`
- Uploads APK as artifact

---

## Useful Gradle Tasks

```bash
# List all tasks
./gradlew tasks

# Dependency analysis
./gradlew dependencies

# Build with parallel execution
./gradlew assembleDebug --parallel

# Build with daemon
./gradlew assembleDebug --daemon
```
