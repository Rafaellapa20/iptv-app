with open('app/src/main/res/layout/activity_main.xml', 'r', encoding='utf-8') as f:
    text = f.read()

count = 0
def rep(old, new):
    global text, count
    if old in text:
        text = text.replace(old, new)
        count += 1
    else:
        print(f"Not found: {old}")

rep('??? IPTV ', '? IPTV ')
rep('???? 1 playlist ativa', '?? 1 playlist ativa')
rep('???? LIVE', '?? LIVE')
rep('???? A DAR: Telejornal', '?? A DAR: Telejornal')
rep('Cat??logo de Filmes ???', 'Cat?logo de Filmes ?')
rep('Temporadas &amp; Epis??dios ???', 'Temporadas &amp; Epis?dios ?')
rep('??  Favoritos', '? Favoritos')
rep('???? Guia EPG', '?? Guia EPG')
rep('??? SpeedTest', '?? Defini??es')
rep('???? Emparelhar', '?? Emparelhar')
rep('??? Defini????es', '?? Modo F?cil')
rep('???? FILMES EM DESTAQUE', '?? FILMES EM DESTAQUE')
rep('???? SOCKS5 VPS PROTECTED', '?? SOCKS5 VPS PROTECTED')
rep('v10.50', 'v10.54')

print(f"Replaced {count} strings")

# Fix button colors just in case
if 'bg_smarters_sage' in text:
    print('Replacing background colors...')
    import re
    text = re.sub(r'(android:id="@+id/btnQuickCatchup"[^>]*?android:background=")@drawable/bg_smarters_sage(")', r'\g<1>#2E7D32\g<2>', text, flags=re.DOTALL)

with open('app/src/main/res/layout/activity_main.xml', 'w', encoding='utf-8') as f:
    f.write(text)

print("Fixed XML directly")
