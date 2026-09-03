# -*- coding: utf-8 -*-
with open('app/src/main/res/layout/activity_main.xml', 'r', encoding='utf-8') as f:
    xml = f.read()

xml = xml.replace('android:text="📻 Rádios"', 'android:text="📱 Emparelhar"')
xml = xml.replace('android:text="🔄 Definições"', 'android:text="🟢 Modo Fácil"')

with open('app/src/main/res/layout/activity_main.xml', 'w', encoding='utf-8') as f:
    f.write(xml)

print("Done")
