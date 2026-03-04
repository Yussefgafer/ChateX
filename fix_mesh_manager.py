import sys

with open('app/src/main/java/com/kai/ghostmesh/core/mesh/MeshManager.kt', 'r') as f:
    content = f.read()

target = """                routingNodes.map { (nodeId, route) ->
                    val profileEntity = currentRepo.getProfile(nodeId)
                    UserProfile(
                        id = nodeId,
                        name = profileEntity?.name ?: "Unknown Peer",
                        status = profileEntity?.status ?: "Active on network",
                        color = profileEntity?.color ?: 0xFF00FF7F.toInt(),
                        batteryLevel = route.battery,
                        isOnline = true,
                        bestEndpoint = route.nextHopEndpointId,
                        transportType = route.nextHopEndpointId.split(":").firstOrNull()
                    )
                }"""

replacement = """                val profiles = mutableListOf<UserProfile>()
                routingNodes.forEach { (nodeId, route) ->
                    val profileEntity = currentRepo.getProfile(nodeId)
                    profiles.add(UserProfile(
                        id = nodeId,
                        name = profileEntity?.name ?: "Unknown Peer",
                        status = profileEntity?.status ?: "Active on network",
                        color = profileEntity?.color ?: 0xFF00FF7F.toInt(),
                        batteryLevel = route.battery,
                        isOnline = true,
                        bestEndpoint = route.nextHopEndpointId,
                        transportType = route.nextHopEndpointId.split(":").firstOrNull()
                    ))
                }
                profiles"""

if target in content:
    new_content = content.replace(target, replacement)
    with open('app/src/main/java/com/kai/ghostmesh/core/mesh/MeshManager.kt', 'w') as f:
        f.write(new_content)
    print("Successfully fixed MeshManager.kt")
else:
    print("Target block not found in MeshManager.kt")
