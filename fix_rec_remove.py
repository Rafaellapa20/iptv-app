# -*- coding: utf-8 -*-
with open('app/src/main/java/com/iptv/app/VodNetflixActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

import re
text = re.sub(r'// --- RECOMENDADOS PARA SI ---.*?// Grupos de categorias', '// Grupos de categorias', text, flags=re.DOTALL)

with open('app/src/main/java/com/iptv/app/VodNetflixActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)
print("Done")
