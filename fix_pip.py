# -*- coding: utf-8 -*-
import re

with open('app/src/main/AndroidManifest.xml', 'r', encoding='utf-8') as f:
    text = f.read()

text = text.replace('<activity\n            android:name=".PlayerActivity"\n            android:exported="false"', '<activity\n            android:name=".PlayerActivity"\n            android:exported="false"\n            android:resizeableActivity="true"\n            android:supportsPictureInPicture="true"')
text = text.replace('<activity\n            android:name=".PlayerActivity"\n            android:configChanges="orientation|keyboardHidden|screenSize"\n            android:exported="false"', '<activity\n            android:name=".PlayerActivity"\n            android:configChanges="orientation|keyboardHidden|screenSize"\n            android:resizeableActivity="true"\n            android:supportsPictureInPicture="true"\n            android:exported="false"')
# simpler replace
if 'android:supportsPictureInPicture="true"' not in text:
    text = re.sub(r'<activity\s+android:name="\.PlayerActivity"', '<activity\n            android:name=".PlayerActivity"\n            android:resizeableActivity="true"\n            android:supportsPictureInPicture="true"', text)

with open('app/src/main/AndroidManifest.xml', 'w', encoding='utf-8') as f:
    f.write(text)

with open('app/src/main/java/com/iptv/app/PlayerActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

pip_code = '''
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            enterPictureInPictureMode(android.app.PictureInPictureParams.Builder().build())
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            epgContainer.visibility = View.GONE
            llFloatingControls.visibility = View.GONE
            findViewById<androidx.media3.ui.PlayerView>(R.id.player_view)?.useController = false
        } else {
            findViewById<androidx.media3.ui.PlayerView>(R.id.player_view)?.useController = true
        }
    }
'''
if 'onUserLeaveHint' not in text:
    text = text.replace('override fun onDestroy() {', pip_code + '\n    override fun onDestroy() {')

with open('app/src/main/java/com/iptv/app/PlayerActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)
print("Done")
