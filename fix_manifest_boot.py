# -*- coding: utf-8 -*-
with open('app/src/main/AndroidManifest.xml', 'r', encoding='utf-8') as f:
    xml = f.read()

# Add permission if not exists
if 'RECEIVE_BOOT_COMPLETED' not in xml:
    xml = xml.replace('<uses-permission android:name="android.permission.INTERNET" />', '<uses-permission android:name="android.permission.INTERNET" />\n    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />')

# Add receiver
if 'BootReceiver' not in xml:
    receiver = '''
        <receiver android:name=".BootReceiver" android:enabled="true" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
        </receiver>
'''
    xml = xml.replace('</application>', receiver + '    </application>')

with open('app/src/main/AndroidManifest.xml', 'w', encoding='utf-8') as f:
    f.write(xml)

print("Manifest updated")
