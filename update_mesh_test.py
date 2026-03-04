import sys

with open('app/src/test/java/com/kai/ghostmesh/mesh/MeshManagerTest.kt', 'r') as f:
    content = f.read()

if 'import org.robolectric.annotation.Config' not in content:
    content = content.replace('import org.robolectric.RobolectricTestRunner', 'import org.robolectric.RobolectricTestRunner\nimport org.robolectric.annotation.Config')

if '@Config(manifest = Config.NONE)' not in content:
    content = content.replace('@RunWith(RobolectricTestRunner::class)', '@RunWith(RobolectricTestRunner::class)\n@Config(manifest = Config.NONE)')

with open('app/src/test/java/com/kai/ghostmesh/mesh/MeshManagerTest.kt', 'w') as f:
    f.write(content)
