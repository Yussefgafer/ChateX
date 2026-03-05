import sys

def replace_in_file(file_path, search_str, replace_str):
    with open(file_path, 'r') as f:
        content = f.read()
    new_content = content.replace(search_str, replace_str)
    with open(file_path, 'w') as f:
        f.write(new_content)

# Fix ExpressiveComponents.kt
# We want the pattern:
# val androidPath = android.graphics.Path()
# morph.toPath(progress, androidPath)
# val path = androidPath.asComposePath()
# drawPath(path, ...)

expressive_path = 'app/src/main/java/com/kai/ghostmesh/core/ui/components/ExpressiveComponents.kt'
with open(expressive_path, 'r') as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if 'val path = Path()' in line:
        # Check if we are inside onDrawBehind
        new_lines.append('                    val androidPath = android.graphics.Path()\n')
    elif 'morph.toPath(' in line:
        # Extract the first argument (progress)
        start = line.find('(') + 1
        end = line.find(',')
        progress = line[start:end].strip()
        new_lines.append(f'                    morph.toPath({progress}, androidPath)\n')
        new_lines.append('                    val path = androidPath.asComposePath()\n')
    else:
        new_lines.append(line)

with open(expressive_path, 'w') as f:
    f.writelines(new_lines)

# Fix ChatScreen.kt
chat_path = 'app/src/main/java/com/kai/ghostmesh/features/chat/ChatScreen.kt'
# Remove existing OptIns to start fresh
replace_in_file(chat_path, '@OptIn(ExperimentalMaterial3Api::class)\n', '')
replace_in_file(chat_path, '@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)\n', '')
# Add the combined OptIn
replace_in_file(chat_path, '@Composable\nfun ChatScreen',
                '@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)\n@Composable\nfun ChatScreen')

# Also need OptIn for other functions in ChatScreen.kt if they use experimental APIs
# DiscoveryScreen.kt also uses them.

# Fix ImageUtils.kt - Ensure base64ToBitmap is there
image_utils_path = 'app/src/main/java/com/kai/ghostmesh/core/util/ImageUtils.kt'
with open(image_utils_path, 'r') as f:
    content = f.read()
if 'fun base64ToBitmap' not in content:
    # Add it before the last closing brace
    last_brace = content.rfind('}')
    new_content = content[:last_brace] + """
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
        f.write(new_content)
