# Mesh Protocol & Routing Analysis

## Protocol Overview
The protocol uses JSON-based packets with a 100KB payload limit. It supports multi-hop routing using a path-cost algorithm based on transport type and battery levels.

## Critical Issues & Logical Errors
1. **Single-Path Vulnerability:**
   - `MeshEngine` only stores one `Route` per destination. If a link breaks, the system has no alternative path cached and must wait for a new `LINK_STATE` or `PROFILE_SYNC` to rediscover the node.
   - **Recommendation:** Implement Multi-path routing (store top 3 routes).

2. **Aggressive WiFi Direct Connection:**
   - `WifiDirectTransport` attempts to `connect()` to every peer discovered in `WIFI_P2P_PEERS_CHANGED_ACTION`. This will cause constant group negotiation failures and battery drain.
   - **Recommendation:** Implement a leader-election or manual connection trigger for WiFi Direct.

3. **Incomplete Gateway Logic:**
   - `tunnelToGateway` simply picks the "first" gateway. It doesn't check if that gateway is actually reachable or has the best connection to the Nostr bridge.
   - **Recommendation:** Add gateway quality metrics (latency/success rate) to the routing table.

4. **Ack Loops/Flooding:**
   - While `processedPacketIds` (LRU set) prevents immediate loops, a packet with a long hop count (10) in a dense network could still cause significant "noise" traffic before being pruned.
   - **Recommendation:** Reduce default `hopCount` to 3-5 and implement "Horizon-based" flooding.

5. **Missing Heartbeats:**
   - Routes are pruned after 10 minutes of inactivity. There is no active "Heartbeat" to keep routes alive, meaning a silent node will disappear from the routing table even if it's still nearby.
   - **Recommendation:** Implement periodic low-power BATTERY_HEARTBEAT packets.
