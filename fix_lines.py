with open('app/src/main/res/layout/activity_main.xml', 'r', encoding='utf-8') as f:
    lines = f.readlines()

for i in range(len(lines)):
    context = ''.join(lines[max(0, i-15):i])
    if 'android:nextFocusRight="@id/btnQuickCatchup"' in lines[i] and 'btnQuickSettings' in context:
        lines[i] = lines[i].replace('@id/btnQuickCatchup', '@id/btnQuickRadios')
    if 'android:nextFocusLeft="@id/btnQuickSettings"' in lines[i] and 'btnQuickCatchup' in context:
        lines[i] = lines[i].replace('@id/btnQuickSettings', '@id/btnQuickRadios')

with open('app/src/main/res/layout/activity_main.xml', 'w', encoding='utf-8') as f:
    f.writelines(lines)

with open('app/build.gradle', 'r', encoding='utf-8') as f:
    btext = f.read()
import re
btext = re.sub(r'versionCode 14[0-9]', 'versionCode 145', btext)
btext = re.sub(r'versionName "10.5[0-9]"', 'versionName "10.57"', btext)
with open('app/build.gradle', 'w', encoding='utf-8') as f:
    f.write(btext)

import json
with open('update.json', 'r', encoding='utf-8-sig') as f:
    u = json.load(f)
u['versionCode'] = 145
u['versionName'] = "10.57"
u['apkUrl'] = "http://176.111.109.14/iptv_app/iptv_mobile_v1057.apk"
with open('update.json', 'w', encoding='utf-8') as f:
    json.dump(u, f, indent=2)

print("Fixed lines!")
