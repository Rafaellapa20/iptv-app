# -*- coding: utf-8 -*-
import os

# 1. Remove from MainActivity.kt
with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'r', encoding='utf-8') as f:
    main_text = f.read()

search = '''        findViewById<View>(R.id.btnQuickRadios)?.setOnClickListener {
            startActivity(android.content.Intent(this, RadiosActivity::class.java))
        }'''
main_text = main_text.replace(search, '')

with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(main_text)

# 2. Remove from activity_main.xml
with open('app/src/main/res/layout/activity_main.xml', 'r', encoding='utf-8') as f:
    xml_text = f.read()

import re
search_xml = r'\s*<Button\s+android:id="@+id/btnQuickRadios".*?/>'
xml_text = re.sub(search_xml, '', xml_text, flags=re.DOTALL)

with open('app/src/main/res/layout/activity_main.xml', 'w', encoding='utf-8') as f:
    f.write(xml_text)

# 3. Delete files
try:
    os.remove('app/src/main/java/com/iptv/app/RadiosActivity.kt')
    os.remove('app/src/main/res/layout/activity_radios.xml')
except:
    pass

print("Done")
