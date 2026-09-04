# -*- coding: utf-8 -*-
with open('app/src/main/java/com/iptv/app/RadiosActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

text = text.replace('A ligar a: \\...', 'A ligar a: ...')
text = text.replace('A tocar: \\ 🔊', 'A tocar:  🔊')
text = text.replace('Erro ao reproduzir \\', 'Erro ao reproduzir ')

with open('app/src/main/java/com/iptv/app/RadiosActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)

with open('app/src/main/res/layout/activity_radios.xml', 'r', encoding='utf-8') as f:
    text = f.read()

text = text.replace('android:background="@drawable/bg_main"', 'android:background="#050A14"')

with open('app/src/main/res/layout/activity_radios.xml', 'w', encoding='utf-8') as f:
    f.write(text)
print("Done")
