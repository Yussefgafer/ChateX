import sys

def replace_in_file(file_path, search_str, replace_str):
    with open(file_path, 'r') as f:
        content = f.read()
    new_content = content.replace(search_str, replace_str)
    with open(file_path, 'w') as f:
        f.write(new_content)

replace_in_file('app/src/main/java/com/kai/ghostmesh/features/chat/ChatScreen.kt',
                '@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)\n@OptIn(ExperimentalMaterial3Api::class)',
                '@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)')

replace_in_file('app/src/main/java/com/kai/ghostmesh/features/chat/ChatScreen.kt',
                '@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)\n@OptIn(ExperimentalMaterial3ExpressiveApi::class)',
                '@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)')
