import re

path = 'app/src/main/java/com/kai/ghostmesh/core/ui/components/ExpressiveComponents.kt'
with open(path, 'r') as f:
    content = f.read()

def replace_block(progress_name, content):
    # This regex matches the onDrawBehind block until matrix.reset()
    # It replaces it with the correct android.graphics.Path logic
    pattern = r'onDrawBehind \{\s+val path = Path\(\)\s+morph\.toPath\(' + progress_name + r', path\)'
    replacement = f'''onDrawBehind {{
                    val aPath = android.graphics.Path()
                    morph.toPath({progress_name}, aPath)
                    val path = aPath.asComposePath()'''
    return re.sub(pattern, replacement, content)

content = replace_block('shapeProgress', content)
content = replace_block('animatedProgress', content)

with open(path, 'w') as f:
    f.write(content)
