import os

def fix_expressive():
    path = 'app/src/main/java/com/kai/ghostmesh/core/ui/components/ExpressiveComponents.kt'
    with open(path, 'r') as f:
        lines = f.readlines()

    new_lines = []
    skip_next = False
    for i, line in enumerate(lines):
        if skip_next:
            skip_next = False
            continue

        if 'onDrawBehind {' in line:
            new_lines.append(line)
            # Find the next lines that define path and morph.toPath
            # We want to replace everything until matrix.reset()
            j = i + 1
            while j < len(lines) and 'matrix.reset()' not in lines[j]:
                j += 1

            # Determine which progress variable to use
            progress = 'morphProgress'
            if 'shapeProgress' in ''.join(lines[i:j]): progress = 'shapeProgress'
            elif 'animatedProgress' in ''.join(lines[i:j]): progress = 'animatedProgress'
            elif 'localEasedProgress' in ''.join(lines[i:j]): progress = 'localEasedProgress'

            new_lines.append(f'                    val aPath = android.graphics.Path()\n')
            new_lines.append(f'                    morph.toPath({progress}, aPath)\n')
            new_lines.append(f'                    val path = aPath.asComposePath()\n')

            # Now we need to skip the messy lines we previously inserted
            # We will skip until we hit matrix.reset()
            # The current loop will continue from j

            # To do this effectively with the current loop, we update i via a different method or just use a while loop
            # Let's rewrite the whole thing for safety.
            pass

    # Simplified approach: direct string replacement of the known bad blocks
    with open(path, 'r') as f:
        content = f.read()

    import re

    # Target the blocks between onDrawBehind and matrix.reset()
    # Replace them with clean code
    pattern = re.compile(r'onDrawBehind \{\s+(?:val path = Path\(\)\n\s+)?val aPath = android\.graphics\.Path\(\); morph\.toPath\(([^,]+), aPath\); val path = aPath\.asComposePath\(\)\n\s+matrix\.reset\(\)', re.MULTILINE)
    # The previous regex might not match exactly due to whitespace or my messy previous edits

    # Let's try a more robust manual replacement
    # Block 1
    content = re.sub(r'onDrawBehind \{\s+val path = Path\(\)\n\s+val aPath = android\.graphics\.Path\(\); morph\.toPath\(morphProgress, aPath\); val path = aPath\.asComposePath\(\)',
                     'onDrawBehind {\n                    val aPath = android.graphics.Path()\n                    morph.toPath(morphProgress, aPath)\n                    val path = aPath.asComposePath()', content)

    # Block 2
    content = re.sub(r'onDrawBehind \{\s+val path = Path\(\)\n\s+val aPath = android\.graphics\.Path\(\); morph\.toPath\(shapeProgress, aPath\); val path = aPath\.asComposePath\(\)',
                     'onDrawBehind {\n                    val aPath = android.graphics.Path()\n                    morph.toPath(shapeProgress, aPath)\n                    val path = aPath.asComposePath()', content)

    # Block 3 & 4
    content = re.sub(r'onDrawBehind \{\s+val path = Path\(\)\n\s+val aPath = android\.graphics\.Path\(\); morph\.toPath\(animatedProgress, aPath\); val path = aPath\.asComposePath\(\)',
                     'onDrawBehind {\n                    val aPath = android.graphics.Path()\n                    morph.toPath(animatedProgress, aPath)\n                    val path = aPath.asComposePath()', content)

    with open(path, 'w') as f:
        f.write(content)

def fix_chat():
    path = 'app/src/main/java/com/kai/ghostmesh/features/chat/ChatScreen.kt'
    with open(path, 'r') as f:
        content = f.read()

    # Remove all OptIn annotations
    content = re.sub(r'@file:OptIn\(.*\)\n', '', content)
    content = re.sub(r'@OptIn\(.*\)\n', '', content)

    # Add one at the top
    content = '@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)\n' + content

    with open(path, 'w') as f:
        f.write(content)

fix_expressive()
fix_chat()
