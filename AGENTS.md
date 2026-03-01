# 🛠️ AGENT OPERATING PROTOCOL

## 🎯 Primary Directives
- **Constraint Mastery:** The Infinix Target (84MB RAM) is absolute. Do not load entire files into memory; stream chunks.
- **Surgical Coding:** Adhere to strict MVVM alignment. No business logic in UI; no UI context in Repositories.
- **Reference Grade:** Implementation must be architectural hardening (mocking edge cases) + UI Mastery (MD3E).

## 🛡️ Mandatory Rules
1. **@Immutable:** All model classes (`UserProfile`, `Packet`, `Message`) MUST be annotated with `@Immutable`.
2. **Null Safety:** Never assume Reflection (GSON/Protobuf) respects Kotlin nullability. Use `Packet.isValid()` verification.
3. **Protobuf:** All network packets must use the Protobuf schema. JSON is deprecated for mesh traffic.
4. **Security:** Never log private keys, session secrets, or full seed phrases.
5. **E2EE:** Maintain the "Blind Postman" principle. Gateways and Relays must never have access to cleartext payloads.

## 🧪 Verification Protocol
1. Build with `./gradlew assembleDebug`.
2. Run unit tests with `./gradlew testDebugUnitTest`.
3. Check UI transitions against Material 3 Expressive motion tokens.

---
*Follow the Architect's Vision.*
