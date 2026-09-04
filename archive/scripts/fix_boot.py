# -*- coding: utf-8 -*-
with open('app/src/main/AndroidManifest.xml', 'r', encoding='utf-8') as f:
    text = f.read()

perm = '<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />'
if perm not in text:
    text = text.replace('<uses-permission android:name="android.permission.INTERNET" />', '<uses-permission android:name="android.permission.INTERNET" />\n    ' + perm)

receiver = '''
        <receiver android:name=".BootReceiver" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>
'''
if 'BootReceiver' not in text:
    text = text.replace('</application>', receiver + '\n    </application>')

with open('app/src/main/AndroidManifest.xml', 'w', encoding='utf-8') as f:
    f.write(text)
