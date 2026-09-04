import re

with open('app/src/main/res/layout/activity_main.xml', 'r', encoding='utf-8') as f:
    xml = f.read()

# Replace all mojibake with the right text
xml = re.sub(r'android:text=".*IPTV "', 'android:text="▼ IPTV "', xml)
xml = re.sub(r'android:text=".*1 playlist ativa"', 'android:text="🟢 1 playlist ativa"', xml)
xml = re.sub(r'android:text=".* LIVE"', 'android:text="🔴 LIVE"', xml)
xml = re.sub(r'android:text=".* A DAR: Telejornal"', 'android:text="🔴 A DAR: Telejornal"', xml)
xml = re.sub(r'android:text="Cat.*logo de Filmes .*"', 'android:text="Catálogo de Filmes ▸"', xml)
xml = re.sub(r'android:text="Temporadas &amp; Epis.*dios .*"', 'android:text="Temporadas &amp; Episódios ▸"', xml)
xml = re.sub(r'android:text=".* FILMES EM DESTAQUE"', 'android:text="🎬 FILMES EM DESTAQUE"', xml)
xml = re.sub(r'android:text=".* SOCKS5 VPS PROTECTED"', 'android:text="🔒 SOCKS5 VPS PROTECTED"', xml)

# For the buttons, use their IDs to ensure correctness
xml = re.sub(r'(android:id="@+id/btnQuickFavorites"[^>]*?android:text=").*?(")', r'\g<1>⭐ Favoritos\g<2>', xml, flags=re.DOTALL)
xml = re.sub(r'(android:id="@+id/btnQuickEpg"[^>]*?android:text=").*?(")', r'\g<1>📺 Guia EPG\g<2>', xml, flags=re.DOTALL)
xml = re.sub(r'(android:id="@+id/btnQuickSettings"[^>]*?android:text=").*?(")', r'\g<1>⚙️ Definições\g<2>', xml, flags=re.DOTALL)
xml = re.sub(r'(android:id="@+id/btnQuickRadios"[^>]*?android:text=").*?(")', r'\g<1>📱 Emparelhar\g<2>', xml, flags=re.DOTALL)
xml = re.sub(r'(android:id="@+id/btnQuickCatchup"[^>]*?android:text=").*?(")', r'\g<1>👴 Modo Fácil\g<2>', xml, flags=re.DOTALL)
xml = re.sub(r'(android:id="@+id/btnQuickCatchup"[^>]*?android:background=")@drawable/bg_smarters_sage(")', r'\g<1>#2E7D32\g<2>', xml, flags=re.DOTALL)

with open('app/src/main/res/layout/activity_main.xml', 'w', encoding='utf-8') as f:
    f.write(xml)

print("Fixed XML completely")
