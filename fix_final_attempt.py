import re

def fix_expressive():
    path = 'app/src/main/java/com/kai/ghostmesh/core/ui/components/ExpressiveComponents.kt'
    with open(path, 'r') as f:
        content = f.read()

    # 1. Ensure all necessary imports
    if 'import androidx.compose.ui.graphics.asAndroidPath' not in content:
        content = content.replace('import androidx.compose.ui.graphics.*',
                                 'import androidx.compose.ui.graphics.*\nimport androidx.compose.ui.graphics.asAndroidPath')

    # 2. Use the most robust form: val path = Path(); morph.toPath(progress, path.asAndroidPath())
    # This works because graphics-shapes toPath expects android.graphics.Path,
    # and asAndroidPath() gives exactly that from a Compose Path.

    # Clean up all previous onDrawBehind content
    # We match from onDrawBehind { until matrix.reset()
    def replacement(match):
        progress = match.group(1)
        return f"""onDrawBehind {{
                    val path = Path()
                    morph.toPath({progress}, path.asAndroidPath())

                    matrix.reset()"""

    # Match various messy states
    content = re.sub(r'onDrawBehind \{\s+.*?morph\.toPath\(([^,]+),.*?\).*?matrix\.reset\(\)', replacement, content, flags=re.DOTALL)

    with open(path, 'w') as f:
        f.write(content)

def fix_chat():
    path = 'app/src/main/java/com/kai/ghostmesh/features/chat/ChatScreen.kt'
    with open(path, 'r') as f:
        lines = f.readlines()

    new_lines = []
    # Add explicit imports for the OptIns
    new_lines.append('import androidx.compose.material3.ExperimentalMaterial3Api\n')
    new_lines.append('import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi\n')
    new_lines.append('@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)\n')

    for line in lines:
        if '@OptIn' in line or '@file:OptIn' in line or 'import androidx.compose.material3.Experimental' in line:
            continue
        new_lines.append(line)

    with open(path, 'w') as f:
        f.writelines(new_lines)

fix_expressive()
fix_chat()
