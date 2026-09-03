# -*- coding: utf-8 -*-
with open('app/src/main/java/com/iptv/app/PlayerActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

import re

# Remove the broken replaced code
broken_code = '''    // replaced: override fun onBackPressed() {
        super.onBackPressed()
    }'''

text = text.replace(broken_code, '')

with open('app/src/main/java/com/iptv/app/PlayerActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)
print("Done")
