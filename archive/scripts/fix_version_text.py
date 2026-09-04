import re

with open('app/src/main/res/layout/activity_main.xml', 'r', encoding='utf-8') as f:
    xml = f.read()

# Change the hardcoded v10.55 to an ID so we can set it in code, or just update the hardcoded text to v10.61
# Actually, wait, let's just update the hardcoded text for now to avoid dealing with code changes in MainActivity.
xml = re.sub(r'android:text="v10\.5[0-9]"', 'android:text="v10.61"', xml)
xml = re.sub(r'android:text="v10\.6[0-9]"', 'android:text="v10.61"', xml)

with open('app/src/main/res/layout/activity_main.xml', 'w', encoding='utf-8') as f:
    f.write(xml)

print("Updated hardcoded version in XML")

with open('app/build.gradle', 'r', encoding='utf-8') as f:
    btext = f.read()
btext = re.sub(r'versionCode 14[0-9]', 'versionCode 149', btext)
btext = re.sub(r'versionName "10.6[0-9]"', 'versionName "10.61"', btext)
btext = re.sub(r'versionName "10.5[0-9]"', 'versionName "10.61"', btext)
with open('app/build.gradle', 'w', encoding='utf-8') as f:
    f.write(btext)

import json
with open('update.json', 'r', encoding='utf-8-sig') as f:
    u = json.load(f)
u['versionCode'] = 149
u['versionName'] = "10.61"
u['apkUrl'] = "http://176.111.109.14/iptv_app/iptv_mobile_v1061.apk"
with open('update.json', 'w', encoding='utf-8') as f:
    json.dump(u, f, indent=2)

print("Bumped to 10.61")
