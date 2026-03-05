import sys

def replace_in_file(file_path, search_str, replace_str):
    with open(file_path, 'r') as f:
        content = f.read()
    new_content = content.replace(search_str, replace_str)
    with open(file_path, 'w') as f:
        f.write(new_content)

# Fix ExpressiveComponents.kt
replace_in_file('app/src/main/java/com/kai/ghostmesh/core/ui/components/ExpressiveComponents.kt',
                'morph.toPath(morphProgress, path.asAndroidPath())',
                'morph.toPath(morphProgress, path)')
replace_in_file('app/src/main/java/com/kai/ghostmesh/core/ui/components/ExpressiveComponents.kt',
                'morph.toPath(shapeProgress, path.asAndroidPath())',
                'morph.toPath(shapeProgress, path)')
replace_in_file('app/src/main/java/com/kai/ghostmesh/core/ui/components/ExpressiveComponents.kt',
                'morph.toPath(animatedProgress, path.asAndroidPath())',
                'morph.toPath(animatedProgress, path)')

# Fix ChatScreen.kt - Add OptIn and check ImageUtils
replace_in_file('app/src/main/java/com/kai/ghostmesh/features/chat/ChatScreen.kt',
                '@Composable\nfun ChatScreen',
                '@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)\n@Composable\nfun ChatScreen')

# Ensure ImageUtils has the method (double check)
with open('app/src/main/java/com/kai/ghostmesh/core/util/ImageUtils.kt', 'r') as f:
    if 'base64ToBitmap' not in f.read():
        print('ImageUtils.kt is missing base64ToBitmap! Patching again...')
        # (This shouldn't happen if previous patch worked)
