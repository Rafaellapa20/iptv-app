with open('app/src/main/res/layout/activity_main.xml', 'r', encoding='utf-8') as f:
    text = f.read()

reps = {
    '??? IPTV ': '? IPTV ',
    '???? 1 playlist ativa': '?? 1 playlist ativa',
    '???? LIVE': '?? LIVE',
    '???? A DAR:': '?? A DAR:',
    'Cat??logo de Filmes ???': 'Cat?logo de Filmes ?',
    'Temporadas &amp; Epis??dios ???': 'Temporadas &amp; Epis?dios ?',
    '??  Favoritos': '? Favoritos',
    '???? Guia EPG': '?? Guia EPG',
    '??? SpeedTest': '?? Defini??es',
    '???? Emparelhar': '?? Emparelhar',
    '??? Defini????es': '?? Modo F?cil',
    '???? FILMES EM DESTAQUE': '?? FILMES EM DESTAQUE',
    '???? SOCKS5 VPS PROTECTED': '?? SOCKS5 VPS PROTECTED',
    'v10.50': 'v10.54',
    'v10.53': 'v10.54'
}

for k, v in reps.items():
    text = text.replace(k, v)

with open('app/src/main/res/layout/activity_main.xml', 'w', encoding='utf-8') as f:
    f.write(text)

print("Fixed XML hard")
