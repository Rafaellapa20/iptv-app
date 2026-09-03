# -*- coding: utf-8 -*-
with open('app/src/main/res/layout/activity_main.xml', 'r', encoding='utf-8') as f:
    xml = f.read()

xml = xml.replace('android:text="v9.1"', 'android:text="v10.50"')

with open('app/src/main/res/layout/activity_main.xml', 'w', encoding='utf-8') as f:
    f.write(xml)

print("Fixed hardcoded version")
