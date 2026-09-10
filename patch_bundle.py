import re

def patch_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()
    
    if "bundle {" in content:
        print(f"{filepath} already has bundle block")
        return
        
    # Find android block
    match = re.search(r'android\s*\{', content)
    if not match:
        print(f"No android block found in {filepath}")
        return
        
    insertion = """
    bundle {
        language {
            enableSplit = false
        }
    }
"""
    # insert after android {
    idx = match.end()
    new_content = content[:idx] + insertion + content[idx:]
    with open(filepath, 'w') as f:
        f.write(new_content)
    print(f"Patched {filepath}")

patch_file('mobile/build.gradle.kts')
patch_file('wear/build.gradle.kts')
