import os
for file in ['app/build.gradle', 'app/src/main/res/layout/activity_main.xml']:
    with open(file, 'rb') as f:
        content = f.read()
    if content.startswith(b'\xef\xbb\xbf'):
        content = content[3:]
    with open(file, 'wb') as f:
        f.write(content)
print("BOM removed")
