# UI/UX & Quality of Life (QoL) Analysis

## Visual & Interaction Design
- **Fidget Physics:** Excellent implementation of `magneticClickable` and `physicalTilt`. Adds a unique "premium" feel.
- **Radar View:** Visually appealing but lacks "Empty State" feedback (e.g., "Searching for nodes...").

## Missing Features / Improvements
1. **Message Status UI:**
   - The UI shows "Delivered" if an ACK is received, but there is no "Read" receipt logic.
   - **Recommendation:** Add `PacketType.READ_RECEIPT` and update `MessageStatus`.

2. **Profile Personalization:**
   - `onProfileUpdate` in `MeshManager` is a TODO. Users cannot see avatars or custom status messages of others yet.
   - **Recommendation:** Implement profile sync packet handling.

3. **Offline Mode Clarity:**
   - Since the app is "Offline First", users need clear indicators of *how* they are connected (BT, LAN, Cloud).
   - **Recommendation:** Add transport icons to the Chat and Discovery screens.

4. **File Transfer UI:**
   - There's no global "Downloads" or "Transfers" view to monitor active file sends/receives.
   - **Recommendation:** Create a `TransferHub` screen.

5. **Haptic Feedback:**
   - While `HapticButton` exists, many interactions (like receiving a message or a node appearing on radar) lack tactile feedback.
