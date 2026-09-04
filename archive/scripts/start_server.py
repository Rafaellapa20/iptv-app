# -*- coding: utf-8 -*-
with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

import re
search = r'super\.onCreate\(savedInstanceState\)\s*setContentView\(R\.layout\.activity_main\)'
replace = '''super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Start TV Server for Remote Control Mode
        RemoteManager.startTvServer(this, username, password)'''

text = re.sub(search, replace, text, count=1)

with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)

print("Done")
