import sys

with open('app/src/main/java/com/kai/ghostmesh/core/data/repository/GhostRepository.kt', 'r') as f:
    content = f.read()

# Remove import
content = content.replace('import android.util.LruCache', '')

# Replace cache definition
old_cache = 'private val metaCache = android.util.LruCache<String, Map<String, Any>>(200)'
new_cache = '''private val metaCache = java.util.Collections.synchronizedMap(
        object : java.util.LinkedHashMap<String, Map<String, Any>>(200, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Map<String, Any>>?): Boolean = size > 200
        }
    )'''

if old_cache in content:
    content = content.replace(old_cache, new_cache)
    # Also fix usage if necessary. android.util.LruCache uses put/get. LinkedHashMap also uses put/get.
    with open('app/src/main/java/com/kai/ghostmesh/core/data/repository/GhostRepository.kt', 'w') as f:
        f.write(content)
    print("Successfully updated GhostRepository.kt cache")
else:
    print("Could not find LruCache definition in GhostRepository.kt")
