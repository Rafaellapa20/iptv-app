# -*- coding: utf-8 -*-
with open('app/src/main/java/com/iptv/app/PlayerActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

# Fix OK button in Zapping
search_ok = '''                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    if (!isControlsVisible) {
                        showOverlays()
                        return true
                    }
                    return super.dispatchKeyEvent(event)
                }'''

replace_ok = '''                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    if (isMiniGuiaVisible) {
                        return super.dispatchKeyEvent(event)
                    }
                    if (!isControlsVisible) {
                        showOverlays()
                        return true
                    }
                    return super.dispatchKeyEvent(event)
                }'''
text = text.replace(search_ok, replace_ok)

# Fix Left/Right for Live TV (Don't show EPG overlays)
search_right = '''                    } else {
                        showOverlays()
                    }
                    return true'''

replace_right = '''                    } else {
                        return super.dispatchKeyEvent(event)
                    }
                    return true'''
text = text.replace(search_right, replace_right)

with open('app/src/main/java/com/iptv/app/PlayerActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)
print("Done")
