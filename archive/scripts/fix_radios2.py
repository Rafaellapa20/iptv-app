# -*- coding: utf-8 -*-
with open('app/src/main/java/com/iptv/app/RadiosActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

import re
search = r'private val radioStations = listOf\(.*?\)\n'

# Find the end of the original list of radios.
# It ends at the first ) but wait, there are ) inside RadioStation(...).
# Let's just do a clean string replacement of everything between private val radioStations and override fun onCreate.

search = r'private val radioStations = listOf\(.*?override fun onCreate'
replace = '''private val radioStations = listOf(
        RadioStation("Rádio Comercial", "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1a/R%C3%A1dio_Comercial_logo.svg/800px-R%C3%A1dio_Comercial_logo.svg.png", "https://mcrscast1.mcr.iol.pt/comercial"),
        RadioStation("RFM", "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4b/Logo_RFM.svg/800px-Logo_RFM.svg.png", "http://20853.live.streamtheworld.com/RFMAAC.aac"),
        RadioStation("M80", "https://upload.wikimedia.org/wikipedia/pt/e/eb/Logo_M80_Radio.png", "https://mcrscast.mcr.iol.pt/m80"),
        RadioStation("Cidade FM", "https://upload.wikimedia.org/wikipedia/pt/f/fb/Logo_Cidade.png", "https://mcrscast.mcr.iol.pt/cidadefm"),
        RadioStation("Renascença", "https://upload.wikimedia.org/wikipedia/commons/e/ea/Logo_RR.png", "http://20853.live.streamtheworld.com/RADIO_RENASCENCA.mp3"),
        RadioStation("Mega Hits", "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cd/Mega_Hits_logo.svg/800px-Mega_Hits_logo.svg.png", "http://20853.live.streamtheworld.com/MEGA_HITS.mp3"),
        RadioStation("Antena 1", "https://upload.wikimedia.org/wikipedia/commons/thumb/6/67/Antena_1_Portugal_logo.svg/800px-Antena_1_Portugal_logo.svg.png", "https://radiocast.rtp.pt/antena180a.mp3"),
        RadioStation("TSF", "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d4/TSF_R%C3%A1dio_Not%C3%ADcias_logo.svg/800px-TSF_R%C3%A1dio_Not%C3%ADcias_logo.svg.png", "https://tsfdirecto.tsf.pt/tsf/smil:tsf.smil/playlist.m3u8"),
        RadioStation("Antena 3", "https://upload.wikimedia.org/wikipedia/commons/thumb/1/18/Antena_3_logo.svg/800px-Antena_3_logo.svg.png", "https://radiocast.rtp.pt/antena380a.mp3"),
        RadioStation("Rádio Observador", "https://upload.wikimedia.org/wikipedia/commons/thumb/8/87/Observador_logo.svg/800px-Observador_logo.svg.png", "https://observador.pt/wp-content/themes/observador/assets/radio/livestream.m3u8")
    )

    override fun onCreate'''

text = re.sub(search, replace, text, flags=re.DOTALL)

with open('app/src/main/java/com/iptv/app/RadiosActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)

print("Done")
