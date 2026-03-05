import os
import re

def replace_in_file(file_path, search_str, replace_str):
    if not os.path.exists(file_path):
        return
    with open(file_path, 'r') as f:
        content = f.read()
    new_content = content.replace(search_str, replace_str)
    with open(file_path, 'w') as f:
        f.write(new_content)

# 1. Fix ExpressiveComponents.kt
path_comp = 'app/src/main/java/com/kai/ghostmesh/core/ui/components/ExpressiveComponents.kt'
with open(path_comp, 'r') as f:
    content = f.read()

# Use explicit android.graphics.Path to avoid ambiguity
# We will use the extension function from androidx.graphics.shapes directly
content = re.sub(r'val path = Path\(\)\n\s+morph\.toPath\((.*), path\)',
                 r'val androidPath = android.graphics.Path()\n                    morph.toPath(\1, androidPath)\n                    val path = androidPath.asComposePath()', content)

# Clean up any duplicated or messy logic from previous attempts
content = content.replace('morph.toPath(morphProgress, path.asAndroidPath())',
                          'val aPath = android.graphics.Path(); morph.toPath(morphProgress, aPath); val path = aPath.asComposePath()')
content = content.replace('morph.toPath(shapeProgress, path.asAndroidPath())',
                          'val aPath = android.graphics.Path(); morph.toPath(shapeProgress, aPath); val path = aPath.asComposePath()')
content = content.replace('morph.toPath(animatedProgress, path.asAndroidPath())',
                          'val aPath = android.graphics.Path(); morph.toPath(animatedProgress, aPath); val path = aPath.asComposePath()')

with open(path_comp, 'w') as f:
    f.write(content)

# 2. Fix ChatScreen.kt - Surgical OptIn
path_chat = 'app/src/main/java/com/kai/ghostmesh/features/chat/ChatScreen.kt'
with open(path_chat, 'r') as f:
    lines = f.readlines()

new_lines = []
# Remove all existing OptIn lines to prevent "not repeatable" error
for line in lines:
    if '@OptIn' in line or '@file:OptIn' in line:
        continue
    new_lines.append(line)

# Add single file-level OptIn
new_lines.insert(0, '@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)\n')

with open(path_chat, 'w') as f:
    f.writelines(new_lines)

# 3. Fix DiscoveryScreen.kt - Missing OptIn
path_disc = 'app/src/main/java/com/kai/ghostmesh/features/discovery/DiscoveryScreen.kt'
if os.path.exists(path_disc):
    with open(path_disc, 'r') as f:
        lines = f.readlines()
    if not any('@file:OptIn' in l for l in lines):
        lines.insert(0, '@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)\n')
        with open(path_disc, 'w') as f:
            f.writelines(lines)
