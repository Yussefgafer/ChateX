import os

def replace_in_file(file_path, search_str, replace_str):
    if not os.path.exists(file_path):
        return
    with open(file_path, 'r') as f:
        content = f.read()
    new_content = content.replace(search_str, replace_str)
    with open(file_path, 'w') as f:
        f.write(new_content)

# 1. Fix ExpressiveComponents.kt
expressive_path = 'app/src/main/java/com/kai/ghostmesh/core/ui/components/ExpressiveComponents.kt'
with open(expressive_path, 'r') as f:
    content = f.read()

# Replace the messy path logic with clean Compose Path logic
import re
# Find all onDrawBehind blocks and replace their content
# We look for the pattern we created:
# val androidPath = android.graphics.Path()
# morph.toPath(..., androidPath)
# val path = androidPath.asComposePath()

content = re.sub(r'val androidPath = android\.graphics\.Path\(\)\n\s+morph\.toPath\((.*), androidPath\)\n\s+val path = androidPath\.asComposePath\(\)',
                 r'val path = Path()\n                    morph.toPath(\1, path)', content)

# Also handle cases where I might have changed it to path.asAndroidPath()
content = content.replace('path.asAndroidPath()', 'path')

with open(expressive_path, 'w') as f:
    f.write(content)

# 2. Fix ChatScreen.kt
chat_path = 'app/src/main/java/com/kai/ghostmesh/features/chat/ChatScreen.kt'
with open(chat_path, 'r') as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if line.strip().startswith('@OptIn'):
        continue # Remove all existing OptIns
    if 'fun ChatScreen' in line:
        new_lines.append('@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)\n')
    new_lines.append(line)

# Also add file-level OptIn just to be safe
new_lines.insert(0, '@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)\n')

with open(chat_path, 'w') as f:
    f.writelines(new_lines)

# 3. Fix ImageUtils.kt
image_utils_path = 'app/src/main/java/com/kai/ghostmesh/core/util/ImageUtils.kt'
with open(image_utils_path, 'r') as f:
    content = f.read()
if 'base64ToBitmap' not in content:
    # Find the last } and insert before it
    idx = content.rfind('}')
    content = content[:idx] + """
    fun base64ToBitmap(base64: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }
}
"""
    with open(image_utils_path, 'w') as f:
        f.write(content)
