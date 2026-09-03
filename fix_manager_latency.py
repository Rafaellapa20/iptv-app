# -*- coding: utf-8 -*-
with open('app/src/main/java/com/iptv/app/PlayerManager.kt', 'r', encoding='utf-8') as f:
    text = f.read()

import re

search = '''            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    30000, // min buffer (30s)
                    120000, // max buffer (2 minutes)
                    3500,  // buffer for playback (3.5s)
                    5000   // buffer for playback after rebuffer
                )
                .setBackBuffer(15000, true)
                .build()'''

replace = '''            // Modo ZERO DELAY (Desporto) para Live TV
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(1500, 5000, 500, 1000)
                .setPrioritizeTimeOverSizeThresholds(true)
                .setBackBuffer(5000, true)
                .build()'''

if search in text:
    text = text.replace(search, replace)
else:
    print("Search block not found")

with open('app/src/main/java/com/iptv/app/PlayerManager.kt', 'w', encoding='utf-8') as f:
    f.write(text)
print("Done")
