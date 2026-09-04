# -*- coding: utf-8 -*-
with open('app/src/main/res/layout/activity_mobile_main.xml', 'r', encoding='utf-8') as f:
    xml = f.read()

xml = xml.replace('xmlns:android="http://schemas.android.com/apk/res/android"', 'xmlns:android="http://schemas.android.com/apk/res/android"\n    xmlns:app="http://schemas.android.com/apk/res-auto"')

with open('app/src/main/res/layout/activity_mobile_main.xml', 'w', encoding='utf-8') as f:
    f.write(xml)

print("Done")
