import os

files_to_check = [
    "app/src/main/java/com/kai/ghostmesh/core/security/SecurityManager.kt",
    "app/src/main/java/com/kai/ghostmesh/core/mesh/MeshEngine.kt",
    "app/src/main/java/com/kai/ghostmesh/core/mesh/MeshManager.kt",
    "app/src/main/java/com/kai/ghostmesh/core/model/Models.kt",
    "app/src/main/java/com/kai/ghostmesh/core/ui/components/PhysicsModifiers.kt",
    "app/src/main/java/com/kai/ghostmesh/features/transfer/TransferHub.kt",
    "app/src/main/java/com/kai/ghostmesh/features/discovery/DiscoveryScreen.kt"
]

for f in files_to_check:
    if os.path.exists(f):
        print(f"VERIFIED: {f}")
    else:
        print(f"MISSING: {f}")

# Check specific logic in SecurityManager
with open("app/src/main/java/com/kai/ghostmesh/core/security/SecurityManager.kt", "r") as f:
    content = f.read()
    if "verifyKeystoreIntegrity" in content:
        print("VERIFIED: verifyKeystoreIntegrity implemented in SecurityManager")
    else:
        print("FAILED: verifyKeystoreIntegrity missing in SecurityManager")

# Check specific logic in MeshEngine
with open("app/src/main/java/com/kai/ghostmesh/core/mesh/MeshEngine.kt", "r") as f:
    content = f.read()
    if "validatePacketSchema" in content:
        print("VERIFIED: validatePacketSchema implemented in MeshEngine")
    else:
        print("FAILED: validatePacketSchema missing in MeshEngine")

# Check specific logic in MeshManager
with open("app/src/main/java/com/kai/ghostmesh/core/mesh/MeshManager.kt", "r") as f:
    content = f.read()
    if "exp(1.0 - batteryRatio)" in content:
        print("VERIFIED: Exponential battery scaling implemented in MeshManager")
    else:
        print("FAILED: Exponential battery scaling missing in MeshManager")
