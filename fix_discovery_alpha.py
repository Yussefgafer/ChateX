import sys

def replace_in_file(filepath, search_str, replace_str):
    with open(filepath, 'r') as f:
        content = f.read()
    new_content = content.replace(search_str, replace_str)
    with open(filepath, 'w') as f:
        f.write(new_content)

# DiscoveryScreen.kt
replace_in_file('app/src/main/java/com/kai/ghostmesh/features/discovery/DiscoveryScreen.kt',
                'color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f),',
                'color = MaterialTheme.colorScheme.surfaceContainer,')

replace_in_file('app/src/main/java/com/kai/ghostmesh/features/discovery/DiscoveryScreen.kt',
                '.border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(dynamicRadius))',
                '.border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(dynamicRadius))')
