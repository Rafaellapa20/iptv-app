# -*- coding: utf-8 -*-
with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'r', encoding='utf-8') as f:
    kt = f.read()

import re
search = r'val apkUrl = "[^"]+"'
replace = 'val apkUrl = "https://tinyurl.com/2adr7sz4"'

kt = re.sub(search, replace, kt)

with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(kt)

print("Done")
