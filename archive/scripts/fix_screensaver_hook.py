# -*- coding: utf-8 -*-
with open('app/src/main/java/com/iptv/app/PlayerActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

import re

screensaver_code = '''
    private var screensaverJob: kotlinx.coroutines.Job? = null

    private fun startScreensaverTimer() {
        screensaverJob?.cancel()
        screensaverJob = CoroutineScope(Dispatchers.Main).launch {
            delay(300000) // 5 minutes
            startActivity(Intent(this@PlayerActivity, ScreensaverActivity::class.java))
        }
    }

    private fun cancelScreensaverTimer() {
        screensaverJob?.cancel()
    }
'''

if 'startScreensaverTimer' not in text:
    text = text.replace('private var diagnosticJob: kotlinx.coroutines.Job? = null', screensaver_code + '\n    private var diagnosticJob: kotlinx.coroutines.Job? = null')

search_play = 'override fun onPlaybackStateChanged(playbackState: Int) {'
replace_play = '''override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    cancelScreensaverTimer()
                } else {
                    startScreensaverTimer()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {'''

if 'onIsPlayingChanged' not in text:
    text = text.replace(search_play, replace_play)

# also cancel on touch/key
text = text.replace('override fun dispatchKeyEvent(event: KeyEvent): Boolean {', 'override fun dispatchKeyEvent(event: KeyEvent): Boolean {\n        cancelScreensaverTimer()\n        if (!getActivePlayer().isPlaying) startScreensaverTimer()')

with open('app/src/main/java/com/iptv/app/PlayerActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)
print("Done")
