import re

def fix_expressive():
    path = 'app/src/main/java/com/kai/ghostmesh/core/ui/components/ExpressiveComponents.kt'
    with open(path, 'r') as f:
        content = f.read()

    # 1. Ensure we have the right imports
    # We need androidx.compose.ui.graphics.asAndroidPath
    if 'import androidx.compose.ui.graphics.asAndroidPath' not in content:
        content = content.replace('import androidx.compose.ui.graphics.*',
                                 'import androidx.compose.ui.graphics.*\nimport androidx.compose.ui.graphics.asAndroidPath')

    # 2. Fix the toPath calls
    # Pattern: val path = Path(); morph.toPath(progress, path)
    # Change to: val path = Path(); morph.toPath(progress, path.asAndroidPath())

    # First, clean up my previous mess
    content = re.sub(r'val androidPath = android\.graphics\.Path\(\)\n\s+morph\.toPath\((.*), androidPath\)\n\s+val path = androidPath\.asComposePath\(\)',
                     r'val path = Path()\n                    morph.toPath(\1, path)', content)

    # Now apply the asAndroidPath() fix
    content = re.sub(r'morph\.toPath\((.*),\s*path\)', r'morph.toPath(\1, path.asAndroidPath())', content)

    with open(path, 'w') as f:
        f.write(content)

def fix_chat():
    path = 'app/src/main/java/com/kai/ghostmesh/features/chat/ChatScreen.kt'
    with open(path, 'r') as f:
        content = f.read()

    # Remove all @OptIn lines
    content = re.sub(r'@OptIn\(.*\)\n', '', content)
    content = re.sub(r'@file:OptIn\(.*\)\n', '', content)

    # Add back at file level
    content = '@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)\n' + content

    with open(path, 'w') as f:
        f.write(content)

def fix_image_utils():
    path = 'app/src/main/java/com/kai/ghostmesh/core/util/ImageUtils.kt'
    with open(path, 'r') as f:
        content = f.read()

    if 'fun base64ToBitmap' not in content:
        # Add it properly
        last_brace = content.rfind('}')
        content = content[:last_brace] + """
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
    with open(path, 'w') as f:
        f.write(content)

fix_expressive()
fix_chat()
fix_image_utils()
