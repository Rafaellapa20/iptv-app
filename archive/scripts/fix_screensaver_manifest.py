# -*- coding: utf-8 -*-
with open('app/src/main/AndroidManifest.xml', 'r', encoding='utf-8') as f:
    text = f.read()

activity = '<activity android:name=".ScreensaverActivity" android:theme="@style/Theme.AppCompat.NoActionBar" android:exported="false" android:screenOrientation="landscape" />'

if 'ScreensaverActivity' not in text:
    text = text.replace('</application>', '    ' + activity + '\n    </application>')

with open('app/src/main/AndroidManifest.xml', 'w', encoding='utf-8') as f:
    f.write(text)
