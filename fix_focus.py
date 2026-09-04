import re

with open('app/src/main/res/layout/activity_main.xml', 'r', encoding='utf-8') as f:
    text = f.read()

# Fix btnQuickSettings nextFocusRight
text = re.sub(r'(android:id="@+id/btnQuickSettings"[^>]*?android:nextFocusRight=")@id/btnQuickCatchup(")', r'\g<1>@id/btnQuickRadios\g<2>', text, flags=re.DOTALL)

# Fix btnQuickCatchup nextFocusLeft
text = re.sub(r'(android:id="@+id/btnQuickCatchup"[^>]*?android:nextFocusLeft=")@id/btnQuickSettings(")', r'\g<1>@id/btnQuickRadios\g<2>', text, flags=re.DOTALL)

with open('app/src/main/res/layout/activity_main.xml', 'w', encoding='utf-8') as f:
    f.write(text)

with open('app/build.gradle', 'r', encoding='utf-8') as f:
    btext = f.read()
btext = re.sub(r'versionCode 14[0-9]', 'versionCode 144', btext)
btext = re.sub(r'versionName "10.5[0-9]"', 'versionName "10.56"', btext)
with open('app/build.gradle', 'w', encoding='utf-8') as f:
    f.write(btext)

import json
with open('update.json', 'r', encoding='utf-8-sig') as f:
    u = json.load(f)
u['versionCode'] = 144
u['versionName'] = "10.56"
u['apkUrl'] = "http://176.111.109.14/iptv_app/iptv_mobile_v1056.apk"
with open('update.json', 'w', encoding='utf-8') as f:
    json.dump(u, f, indent=2)

print("Fixed focus!")
