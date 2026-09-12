import re

# Fix mobile
with open('mobile/build.gradle.kts', 'r') as f:
    content = f.read()

content = re.sub(r'versionCode = getGitCommitCount\(\)', 'versionCode = 50', content)

with open('mobile/build.gradle.kts', 'w') as f:
    f.write(content)

# Fix wear
with open('wear/build.gradle.kts', 'r') as f:
    content = f.read()

content = re.sub(r'versionCode = getGitCommitCount\(\) \+ 10000', 'versionCode = 10050', content)

with open('wear/build.gradle.kts', 'w') as f:
    f.write(content)

