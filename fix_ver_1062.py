import re
import json

# Update build.gradle
with open('app/build.gradle', 'r', encoding='utf-8') as f:
    btext = f.read()
btext = re.sub(r'versionCode 14[0-9]', 'versionCode 150', btext)
btext = re.sub(r'versionName "10.6[0-9]"', 'versionName "10.62"', btext)
with open('app/build.gradle', 'w', encoding='utf-8') as f:
    f.write(btext)

# Update activity_main.xml hardcoded text
with open('app/src/main/res/layout/activity_main.xml', 'r', encoding='utf-8') as f:
    xml = f.read()
xml = re.sub(r'android:text="v10\.6[0-9]"', 'android:text="v10.62"', xml)
with open('app/src/main/res/layout/activity_main.xml', 'w', encoding='utf-8') as f:
    f.write(xml)

# Update update.json
with open('update.json', 'r', encoding='utf-8-sig') as f:
    u = json.load(f)
u['versionCode'] = 150
u['versionName'] = "10.62"
u['apkUrl'] = "https://raw.githubusercontent.com/Rafaellapa20/iptv-app/main/iptv_v10.62-debug.apk"
with open('update.json', 'w', encoding='utf-8') as f:
    json.dump(u, f, indent=2)

print("Bumped to 10.62")
