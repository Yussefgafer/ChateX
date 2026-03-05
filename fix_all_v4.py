import os
import re

def fix_expressive():
    path = 'app/src/main/java/com/kai/ghostmesh/core/ui/components/ExpressiveComponents.kt'
    with open(path, 'r') as f:
        content = f.read()

    # 1. Ensure imports
    if 'import androidx.compose.ui.graphics.asAndroidPath' not in content:
        content = content.replace('import androidx.compose.ui.graphics.*',
                                 'import androidx.compose.ui.graphics.*\nimport androidx.compose.ui.graphics.asAndroidPath')

    # 2. Fix the drawing blocks
    # We will use a very specific replacement for each block to be safe

    # CoercedExpressiveCard
    content = re.sub(r'onDrawBehind \{\s+.*?morph\.toPath\(morphProgress,.*?\)',
                     'onDrawBehind {\n                    val path = Path()\n                    morph.toPath(morphProgress, path.asAndroidPath())', content, flags=re.DOTALL)

    # ExpressiveButton
    content = re.sub(r'onDrawBehind \{\s+.*?morph\.toPath\(shapeProgress,.*?\)',
                     'onDrawBehind {\n                    val path = Path()\n                    morph.toPath(shapeProgress, path.asAndroidPath())', content, flags=re.DOTALL)

    # MorphingDiscoveryButton & MD3ELoadingIndicator
    # These use animatedProgress
    content = re.sub(r'onDrawBehind \{\s+.*?morph\.toPath\(animatedProgress,.*?\)',
                     'onDrawBehind {\n                    val path = Path()\n                    morph.toPath(animatedProgress, path.asAndroidPath())', content, flags=re.DOTALL)

    with open(path, 'w') as f:
        f.write(content)

def fix_chat():
    path = 'app/src/main/java/com/kai/ghostmesh/features/chat/ChatScreen.kt'
    with open(path, 'r') as f:
        lines = f.readlines()

    new_lines = []
    # Find package line
    package_idx = -1
    for i, line in enumerate(lines):
        if line.startswith('package '):
            package_idx = i
            break

    if package_idx != -1:
        new_lines.append('@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)\n')
        new_lines.append(lines[package_idx])
        new_lines.append('\nimport androidx.compose.material3.ExperimentalMaterial3Api\n')
        new_lines.append('import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi\n')

        # Add all other lines except package and OptIns
        for i, line in enumerate(lines):
            if i == package_idx: continue
            if '@OptIn' in line or '@file:OptIn' in line or 'import androidx.compose.material3.Experimental' in line:
                continue
            new_lines.append(line)

    with open(path, 'w') as f:
        f.writelines(new_lines)

fix_expressive()
fix_chat()
