import sys

def replace_in_file(filepath, search_str, replace_str):
    with open(filepath, 'r') as f:
        content = f.read()
    new_content = content.replace(search_str, replace_str)
    with open(filepath, 'w') as f:
        f.write(new_content)

# MessagesScreen.kt
replace_in_file('app/src/main/java/com/kai/ghostmesh/features/messages/MessagesScreen.kt',
                'actions = {\n                        ExpressiveIconButton(onClick = onNavigateToRadar) { Icon(Icons.Default.Radar, null) }\n                        ExpressiveIconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, null) }\n                    },',
                'actions = {\n                        ExpressiveIconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, null) }\n                    },')

replace_in_file('app/src/main/java/com/kai/ghostmesh/features/messages/MessagesScreen.kt',
                'Scaffold(\n            containerColor = Color.Transparent,',
                'Scaffold(\n            containerColor = Color.Transparent,\n            floatingActionButton = {\n                MorphingDiscoveryButton(onClick = onNavigateToRadar)\n            },')

# DiscoveryScreen.kt
replace_in_file('app/src/main/java/com/kai/ghostmesh/features/discovery/DiscoveryScreen.kt',
                'floatingActionButton = {\n                MorphingDiscoveryButton(onClick = { onShout("PING") })\n            }',
                'floatingActionButton = {\n                FloatingActionButton(\n                    onClick = { onShout("PING") },\n                    containerColor = MaterialTheme.colorScheme.primaryContainer,\n                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,\n                    shape = CircleShape\n                ) {\n                    Icon(Icons.Default.SignalCellularAlt, "Ping All")\n                }\n            }')
