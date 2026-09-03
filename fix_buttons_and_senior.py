# -*- coding: utf-8 -*-
with open('app/src/main/res/layout/activity_main.xml', 'r', encoding='utf-8') as f:
    xml = f.read()

# Replace Radios text
xml = xml.replace('android:text="📻 Rádios"', 'android:text="📱 Telemóvel (QR)"')
xml = xml.replace('android:text="📻 Rdios"', 'android:text="📱 Telemóvel (QR)"')
# Replace Catchup text
xml = xml.replace('android:text="🔄 Definições"', 'android:text="👴 Modo Idosos"')
xml = xml.replace('android:text="🔄 Definies"', 'android:text="👴 Modo Idosos"')

with open('app/src/main/res/layout/activity_main.xml', 'w', encoding='utf-8') as f:
    f.write(xml)

# Update MainActivity.kt
with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'r', encoding='utf-8') as f:
    kt = f.read()

kt = kt.replace('R.id.btnQuickSenior', 'R.id.btnQuickCatchup')
kt = kt.replace('R.id.btnQrCode', 'R.id.btnQuickRadios')

with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(kt)

# Update SeniorMainActivity.kt
with open('app/src/main/java/com/iptv/app/SeniorMainActivity.kt', 'r', encoding='utf-8') as f:
    senior = f.read()

senior = senior.replace('ChannelsActivity::class.java', 'LiveTvActivity::class.java')

with open('app/src/main/java/com/iptv/app/SeniorMainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(senior)

print("Done")
