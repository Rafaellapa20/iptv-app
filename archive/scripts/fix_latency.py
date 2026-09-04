# -*- coding: utf-8 -*-
with open('app/src/main/java/com/iptv/app/PlayerActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

import re

search = '''        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                30000, // min buffer (30s)
                120000, // max buffer (2 minutes)
                3500,  // buffer for playback (3.5s)
                5000   // buffer for playback after rebuffer
            )
            .setBackBuffer(15000, true)
            .build()'''

replace = '''        val type = intent.getStringExtra("TYPE") ?: "live"
        val isLive = type == "live"
        
        val minBuffer = if (isLive) 1500 else 30000
        val maxBuffer = if (isLive) 5000 else 120000
        val bufferForPlayback = if (isLive) 500 else 3500
        val bufferForPlaybackAfterRebuffer = if (isLive) 1000 else 5000
        
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(minBuffer, maxBuffer, bufferForPlayback, bufferForPlaybackAfterRebuffer)
            .setPrioritizeTimeOverSizeThresholds(isLive)
            .setBackBuffer(if (isLive) 5000 else 15000, true)
            .build()'''

if search in text:
    text = text.replace(search, replace)
else:
    print("Search block not found")

with open('app/src/main/java/com/iptv/app/PlayerActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)
print("Done")
