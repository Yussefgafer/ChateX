import sys

with open('app/src/main/java/com/kai/ghostmesh/core/mesh/MeshManager.kt', 'r') as f:
    content = f.read()

old_block = """                routingNodes.map { (nodeId, route) ->
                    val profileEntity = currentRepo.getProfileSync(nodeId)
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

new_block = """                routingNodes.map { (nodeId, route) ->
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

if old_block in content:
    new_content = content.replace(old_block, new_block)
    with open('app/src/main/java/com/kai/ghostmesh/core/mesh/MeshManager.kt', 'w') as f:
        f.write(new_content)
    print("Successfully updated MeshManager.kt")
else:
    print("Could not find the target block in MeshManager.kt")
