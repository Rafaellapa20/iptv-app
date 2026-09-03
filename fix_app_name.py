# -*- coding: utf-8 -*-
with open('app/src/main/AndroidManifest.xml', 'r', encoding='utf-8') as f:
    xml = f.read()

xml = xml.replace('android:label="MyIPTV"', 'android:label="IPTV Global"')

with open('app/src/main/AndroidManifest.xml', 'w', encoding='utf-8') as f:
    f.write(xml)

print("Done")
