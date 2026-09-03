# -*- coding: utf-8 -*-
with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

import re
# Remove the bad one
text = text.replace('RemoteManager.startTvServer(this, username, password)', '')

# Find where username and password are set
search = r'password = intent\.getStringExtra\("PASSWORD"\) \?: prefs\.getString\("PASSWORD", ""\) \?: ""'
replace = '''password = intent.getStringExtra("PASSWORD") ?: prefs.getString("PASSWORD", "") ?: ""
        
        // Start TV Server for Remote Control Mode
        RemoteManager.startTvServer(this, username, password)'''

text = re.sub(search, replace, text, count=1)

with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)

print("Done")
