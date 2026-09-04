# -*- coding: utf-8 -*-
with open('app/src/main/res/layout/activity_main.xml', 'r', encoding='utf-8') as f:
    xml_text = f.read()

import re

# Remove the Button itself
search_btn = r'(\s*<Button\s+android:id="@+id/btnQuickRadios"[\s\S]*?/>)'
xml_text = re.sub(search_btn, '', xml_text)

# Fix the nextFocus of btnQuickSettings to skip Radios
xml_text = xml_text.replace('android:nextFocusRight="@id/btnQuickRadios"', 'android:nextFocusRight="@id/btnQuickCatchup"')

# Fix the nextFocus of btnQuickCatchup to skip Radios
xml_text = xml_text.replace('android:nextFocusLeft="@id/btnQuickRadios"', 'android:nextFocusLeft="@id/btnQuickSettings"')

with open('app/src/main/res/layout/activity_main.xml', 'w', encoding='utf-8') as f:
    f.write(xml_text)

# Also ensure it's removed from MainActivity.kt
with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'r', encoding='utf-8') as f:
    main_text = f.read()

main_text = re.sub(r'\s*findViewById<View>\(R\.id\.btnQuickRadios\)\?\.setOnClickListener\s*\{\s*startActivity\(android\.content\.Intent\(this,\s*RadiosActivity::class\.java\)\)\s*\}', '', main_text)

with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(main_text)

print("Done")
