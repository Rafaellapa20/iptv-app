# -*- coding: utf-8 -*-
with open('app/src/main/java/com/iptv/app/RadiosActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

text = text.replace('tvNowPlaying.text = "A tocar:  🔊"', 'tvNowPlaying.text = "A tocar: " + station.name + " 🔊"')
text = text.replace('tvNowPlaying.text = "Erro ao reproduzir "', 'tvNowPlaying.text = "Erro ao reproduzir " + station.name')
text = text.replace('tvNowPlaying.text = "A ligar a: ..."', 'tvNowPlaying.text = "A ligar a: " + station.name + "..."')

with open('app/src/main/java/com/iptv/app/RadiosActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)
print("Done")
