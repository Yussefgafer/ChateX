import re

def fix_expressive():
    path = 'app/src/main/java/com/kai/ghostmesh/core/ui/components/ExpressiveComponents.kt'
    with open(path, 'r') as f:
        content = f.read()

    # Ensure imports are clean
    content = content.replace('import android.graphics.Path as AndroidPath', '')

    # Use explicit android.graphics.Path and asComposePath()
    # This is the most reliable way when extension methods are causing ambiguity

    def replacement(match):
        progress = match.group(1)
        return f"""val androidPath = android.graphics.Path()
                    morph.toPath({progress}, androidPath)
                    val path = androidPath.asComposePath()"""

    content = re.sub(r'val path = Path\(\)\n\s+morph\.toPath\((.*), path\)', replacement, content)

    # Clean up any previous failed attempts
    content = content.replace('path.asAndroidPath()', 'path')

    with open(path, 'w') as f:
        f.write(content)

def fix_chat():
    path = 'app/src/main/java/com/kai/ghostmesh/features/chat/ChatScreen.kt'
    with open(path, 'r') as f:
        lines = f.readlines()

    new_lines = []
    new_lines.append('@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)\n')
    for line in lines:
        if '@OptIn' in line: continue
        new_lines.append(line)

    with open(path, 'w') as f:
        f.writelines(new_lines)

fix_expressive()
fix_chat()
