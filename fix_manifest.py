# -*- coding: utf-8 -*-
with open('app/src/main/AndroidManifest.xml', 'r', encoding='utf-8') as f:
    xml = f.read()

senior_activity = '        <activity android:name=".SeniorMainActivity" android:exported="false" android:screenOrientation="landscape" />\n'

if '.SeniorMainActivity' not in xml:
    xml = xml.replace('</application>', senior_activity + '</application>')

with open('app/src/main/AndroidManifest.xml', 'w', encoding='utf-8') as f:
    f.write(xml)

print("Done")
