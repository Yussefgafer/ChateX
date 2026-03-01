# 🛠️ AGENT OPERATING PROCEDURES (ChateX)

## 🏗️ Architecture Standards: Modular Void
1. **Strict Layer Separation**: Never import `features` into `core`. ViewModels must live in their respective `features.[name]` package.
2. **Singleton MeshManager**: All networking logic MUST go through the `MeshManager` singleton provided by `AppContainer`.
3. **Transport Plugin System**: To add a new connection method, implement `MeshTransport` and register it in `MeshManager.init`.
4. **Immutable Models**: All data classes in `.core.model` MUST be annotated with `@Immutable`.

## 🧪 Testing Protocol
1. **Unit Test Everything**: Any new logic in ViewModels, Repositories, or the Mesh Engine MUST have a corresponding unit test in `src/test`.
2. **Mocking Strategy**: Use `MockK` for dependencies. Avoid real database/network calls in unit tests.
3. **Performance Audit**: Ensure animations use `MaterialTheme.motionScheme` spring tokens to maintain 90FPS on low-RAM devices (Target: 84MB baseline).

## 🎨 UI Guidelines (Fidget Physics)
- Use `Modifier.magneticClickable()` for buttons.
- Use `Modifier.physicalTilt()` for cards and surfaces.
- Adhere to the **Midnight Teal / Slate Blue** tonal palette.

## 📡 Packet Protocol
Always use the null-safe `Packet.isValid()` extension before processing any incoming JSON to prevent reflection-based null injection.

---
*Follow these rules to keep the codebase Spectral Clean.*
