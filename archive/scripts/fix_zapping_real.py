# -*- coding: utf-8 -*-
with open('app/src/main/java/com/iptv/app/PlayerActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

import re

search = r'private fun handleNumericZapping.*?override fun dispatchKeyEvent\(event: KeyEvent\): Boolean \{'

replace = '''    private fun handleNumericZapping(keyCode: Int): Boolean {
        val digit = when (keyCode) {
            KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_NUMPAD_0 -> "0"
            KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_NUMPAD_1 -> "1"
            KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_NUMPAD_2 -> "2"
            KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_NUMPAD_3 -> "3"
            KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_NUMPAD_4 -> "4"
            KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_NUMPAD_5 -> "5"
            KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_NUMPAD_6 -> "6"
            KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_NUMPAD_7 -> "7"
            KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_NUMPAD_8 -> "8"
            KeyEvent.KEYCODE_9, KeyEvent.KEYCODE_NUMPAD_9 -> "9"
            else -> return false
        }

        val t = intent.getStringExtra("TYPE")
        if (t != "live") return false // Apenas para Live TV

        channelNumberBuffer += digit
        tvChannelNumber.text = channelNumberBuffer
        tvChannelNumber.visibility = View.VISIBLE

        channelNumberJob?.cancel()
        channelNumberJob = CoroutineScope(Dispatchers.Main).launch {
            delay(2500)
            val num = channelNumberBuffer.toIntOrNull()
            channelNumberBuffer = ""
            tvChannelNumber.visibility = View.GONE

            if (num != null && num > 0) {
                val urls = intent.getStringArrayListExtra("CHANNEL_URLS")
                val names = intent.getStringArrayListExtra("CHANNEL_NAMES")
                val targetIndex = num - 1
                if (urls != null && targetIndex in 0 until urls.size) {
                    intent.putExtra("CURRENT_INDEX", targetIndex)
                    currentStreamUrl = urls[targetIndex]
                    val title = names?.getOrNull(targetIndex) ?: "Canal \"
                    intent.putExtra("TITLE", title)
                    tvLoadingTitle.text = title
                    playUrlInPlayer(getActivePlayer(), currentStreamUrl)
                    hideOverlays()
                } else {
                    Toast.makeText(this@PlayerActivity, "Canal \ n\u00e3o encontrado", Toast.LENGTH_SHORT).show()
                }
            }
        }
        return true
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {'''

text = re.sub(search, replace, text, flags=re.DOTALL)

with open('app/src/main/java/com/iptv/app/PlayerActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)
print("Done")
