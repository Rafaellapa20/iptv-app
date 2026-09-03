# -*- coding: utf-8 -*-
with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

import re
text = re.sub(r'findViewById<View>\(R\.id\.btnQuickMultiScreen\)\?\.setOnClickListener \{.*?\n\s*\}\n', '', text, flags=re.DOTALL)

with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)
print("Done")
