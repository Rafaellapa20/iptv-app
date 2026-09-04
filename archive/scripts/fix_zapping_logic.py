# -*- coding: utf-8 -*-
with open('app/src/main/java/com/iptv/app/PlayerActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

# Add variable declaration
var_decl = '''    private lateinit var tvDiagnostics: android.widget.TextView
    private lateinit var tvChannelNumber: android.widget.TextView
    private var channelNumberBuffer = ""
    private var channelNumberJob: kotlinx.coroutines.Job? = null'''

text = text.replace('private lateinit var tvDiagnostics: android.widget.TextView', var_decl)

# Add findViewById
find_view = '''        tvDiagnostics = findViewById(R.id.tvDiagnostics)
        tvChannelNumber = findViewById(R.id.tvChannelNumber)'''

text = text.replace('tvDiagnostics = findViewById(R.id.tvDiagnostics)', find_view)

# Add onKeyDown logic
# We need to intercept key events. PlayerActivity has dispatchKeyEvent.
# We will inject our numpad logic at the beginning of dispatchKeyEvent.

key_logic = '''
    private fun handleNumericZapping(keyCode: Int): Boolean {
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

        if (type != "live") return false // Apenas para Live TV

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
                // Procurar canal pelo nÃºmero (Ã­ndice 1-based) na lista atual
                val targetIndex = num - 1
                if (targetIndex in 0 until allChannels.size) {
                    currentChannelIndex = targetIndex
                    playChannel(allChannels[targetIndex])
                } else {
                    Toast.makeText(this@PlayerActivity, "Canal \ nÃ£o encontrado", Toast.LENGTH_SHORT).show()
                }
            }
        }
        return true
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        cancelScreensaverTimer()
        if (!getActivePlayer().isPlaying) startScreensaverTimer()
        
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (handleNumericZapping(event.keyCode)) return true
        }
'''

text = text.replace('''override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        cancelScreensaverTimer()
        if (!getActivePlayer().isPlaying) startScreensaverTimer()''', key_logic)

with open('app/src/main/java/com/iptv/app/PlayerActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)
print("Done")
