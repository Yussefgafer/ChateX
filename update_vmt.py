import sys

with open('app/src/test/java/com/kai/ghostmesh/features/ViewModelsTest.kt', 'r') as f:
    content = f.read()

if 'import org.robolectric.annotation.Config' not in content:
    content = content.replace('import org.robolectric.RobolectricTestRunner', 'import org.robolectric.RobolectricTestRunner\nimport org.robolectric.annotation.Config')

content = content.replace('@RunWith(RobolectricTestRunner::class)', '@RunWith(RobolectricTestRunner::class)\n@Config(manifest = Config.NONE)')

with open('app/src/test/java/com/kai/ghostmesh/features/ViewModelsTest.kt', 'w') as f:
    f.write(content)
