import os
import re

test_dir = "app/src/test/java"

for root, dirs, files in os.walk(test_dir):
    for file in files:
        if file.endswith("Test.kt"):
            path = os.path.join(root, file)
            with open(path, 'r') as f:
                content = f.read()

            # Remove the @Ignore before the class
            content = re.sub(r'@Ignore\n(class\s+)', r'\1', content, count=1)

            with open(path, 'w') as f:
                f.write(content)
            print(f"Reverted {path}")
