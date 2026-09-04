with open('app/build.gradle', 'r', encoding='utf-8') as f:
    btext = f.read()
import re
btext = re.sub(r'versionCode 14[0-9]', 'versionCode 148', btext)
btext = re.sub(r'versionName "10.5[0-9]"', 'versionName "10.60"', btext)
with open('app/build.gradle', 'w', encoding='utf-8') as f:
    f.write(btext)

import json
with open('update.json', 'r', encoding='utf-8-sig') as f:
    u = json.load(f)
u['versionCode'] = 148
u['versionName'] = "10.60"
u['apkUrl'] = "http://176.111.109.14/iptv_app/iptv_mobile_v1060.apk"
with open('update.json', 'w', encoding='utf-8') as f:
    json.dump(u, f, indent=2)

print("Bumped to 10.60")
