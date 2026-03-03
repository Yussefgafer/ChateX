# Commercial Readiness & Distribution Audit

## Checklist for Production
- [ ] **Performance:** `RadarView` needs optimization for 100+ nodes (currently uses `Canvas` but draws links for ALL nodes every frame).
- [ ] **Battery:** Aggressive WiFi Direct and Bluetooth scanning will drain battery in <4 hours. Needs a "Low Power Mode" that throttles scanning further.
- [ ] **Accessibility:** Semantic `contentDescription` is present in `RadarView` but missing in many custom components like `MorphingIcon`.
- [ ] **Internationalization (i18n):** Hardcoded strings found in many ViewModels (e.g., "Encrypted message", "The public spectral channel").
- [ ] **Error Handling:** Many network operations use empty `catch` blocks or just log errors. Need user-facing "Toast" or "Snackbar" feedback.

## Distribution Readiness
1. **ProGuard/R8:** Native libraries (Secp256k1) and Gson reflection need strict R8 rules to prevent crashes in Release builds.
2. **Permissions:** The app requires many sensitive permissions (Location, Bluetooth, Nearby). The "Permission Request" flow should be more "Educational" (explaining WHY they are needed).
3. **App Size:** Including native libraries for multiple architectures (arm64, x86_64) will increase APK size. Ensure ABI splitting is enabled.

## Final Verdict
The app has a "High Quality" core but is currently in a **Beta/Prototype** state. The lack of signature verification and the coupling of file transfers are the biggest blockers for a commercial release.
