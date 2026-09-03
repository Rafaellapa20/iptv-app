# -*- coding: utf-8 -*-
import re

# 1. Update UpdateManager
with open('app/src/main/java/com/iptv/app/UpdateManager.kt', 'r', encoding='utf-8') as f:
    kt = f.read()

kt = kt.replace('"https://raw.githubusercontent.com/Rafaellapa20/iptv-app/main/update.json"', '"http://176.111.109.14/iptv_app/update.json"')

with open('app/src/main/java/com/iptv/app/UpdateManager.kt', 'w', encoding='utf-8') as f:
    f.write(kt)

# 2. Update MainActivity QR Code
with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'r', encoding='utf-8') as f:
    kt2 = f.read()

kt2 = re.sub(r'val apkUrl = "[^"]+"', 'val apkUrl = "https://tinyurl.com/2985xryp"', kt2)

with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(kt2)

print("Updated KT files")
