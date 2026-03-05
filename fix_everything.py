import os
import re

def fix_expressive():
    path = 'app/src/main/java/com/kai/ghostmesh/core/ui/components/ExpressiveComponents.kt'
    with open(path, 'r') as f:
        content = f.read()

    # 1. Simplify drawing blocks to use Compose Path
    # Based on the error, there's a toPath extension that expects Compose Path.
    def clean_draw_block(progress):
        return f"""onDrawBehind {{
                    val path = Path()
                    morph.toPath({progress}, path)"""

    # Replace all onDrawBehind blocks with the simplest form
    content = re.sub(r'onDrawBehind \{\s+.*?morph\.toPath\(([^,]+),.*?\)',
                     lambda m: clean_draw_block(m.group(1)), content, flags=re.DOTALL)

    # Clean up duplicated variables or syntax errors
    content = content.replace('val path = Path()', 'val path = Path()') # No-op
    content = content.replace('val path = aPath.asComposePath()', '')
    content = content.replace('val androidPath = android.graphics.Path()', '')

    # Final cleanup of any potential syntax mess
    content = re.sub(r'val path = Path\(\)\n\s+val path = Path\(\)', r'val path = Path()', content)

    with open(path, 'w') as f:
        f.write(content)

def fix_chat_screen():
    path = 'app/src/main/java/com/kai/ghostmesh/features/chat/ChatScreen.kt'
    with open(path, 'r') as f:
        content = f.read()

    # Clean up all annotations
    content = re.sub(r'@file:OptIn\(.*?\)\n', '', content)
    content = re.sub(r'@OptIn\(.*?\)\n', '', content)

    # Add one clean file-level OptIn
    content = '@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)\n' + content

    with open(path, 'w') as f:
        f.write(content)

def fix_main_activity():
    path = 'app/src/main/java/com/kai/ghostmesh/MainActivity.kt'
    with open(path, 'r') as f:
        lines = f.readlines()

    new_lines = []
    has_chat_import = False
    for line in lines:
        if 'import com.kai.ghostmesh.features.chat.ChatScreen' in line:
            has_chat_import = True
        new_lines.append(line)

    if not has_chat_import:
        # Insert after package
        for i, line in enumerate(new_lines):
            if line.startswith('package '):
                new_lines.insert(i + 1, 'import com.kai.ghostmesh.features.chat.ChatScreen\n')
                new_lines.insert(i + 2, 'import com.kai.ghostmesh.features.chat.ChatViewModel\n')
                break

    # Fix the call site
    full_content = "".join(new_lines)
    if 'stagedMedia = ' not in full_content:
        full_content = full_content.replace('onClearReply = { chatViewModel.clearReply() },',
            '''onClearReply = { chatViewModel.clearReply() },
                                    stagedMedia = stagedMedia,
                                    onStageMedia = { uri, type -> chatViewModel.stageMedia(uri, type) },
                                    onUnstageMedia = { chatViewModel.unstageMedia(it) },
                                    recordingDuration = recordingDuration,
                                    cornerRadius = currentCornerRadius,
                                    transportType = currentTransport,''')

    # Ensure variables are defined
    if 'val stagedMedia by chatViewModel.stagedMedia' not in full_content:
        full_content = full_content.replace('val chatMessages by chatViewModel.messages.collectAsState()',
            '''val chatMessages by chatViewModel.messages.collectAsState()
                                val stagedMedia by chatViewModel.stagedMedia.collectAsState()
                                val recordingDuration by chatViewModel.recordingDuration.collectAsState()
                                val currentCornerRadius by settingsViewModel.cornerRadius.collectAsState()
                                val currentTransport = ""''')

    with open(path, 'w') as f:
        f.write(full_content)

fix_expressive()
fix_chat_screen()
fix_main_activity()
