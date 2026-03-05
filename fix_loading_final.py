import sys

def replace_in_file(filepath, search_str, replace_str):
    with open(filepath, 'r') as f:
        content = f.read()
    new_content = content.replace(search_str, replace_str)
    with open(filepath, 'w') as f:
        f.write(new_content)

replace_in_file('app/src/main/java/com/kai/ghostmesh/MainActivity.kt',
                'if (true) { // Bypassing blocking gate',
                '')

# Note: This will leave a trailing } but I will fix it in the next call or manually
