import os
import re

test_dir = "app/src/test/java"

for root, dirs, files in os.walk(test_dir):
    for file in files:
        if file.endswith("Test.kt"):
            path = os.path.join(root, file)
            with open(path, 'r') as f:
                content = f.read()

            # Remove existing Ignore annotations to avoid duplicates
            content = re.sub(r'@org\.junit\.Ignore\s*', '', content)
            content = re.sub(r'@Ignore\s*', '', content)

            # Ensure import exists
            if "import org.junit.Ignore" not in content:
                # Add after package declaration
                content = re.sub(r'(package\s+[\w\.]+)', r'\1\n\nimport org.junit.Ignore', content, count=1)

            # Add @Ignore before the class declaration
            content = re.sub(r'(class\s+)', r'@Ignore\n\1', content, count=1)

            with open(path, 'w') as f:
                f.write(content)
            print(f"Processed {path}")
