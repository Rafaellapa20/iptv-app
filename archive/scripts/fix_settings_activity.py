# -*- coding: utf-8 -*-
import re

with open('app/src/main/java/com/iptv/app/SettingsActivity.kt', 'r', encoding='utf-8') as f:
    kt = f.read()

# Remove switchVpn block
kt = re.sub(r'val switchVpn = findViewById<Switch>\(R\.id\.switchVpn\).*?Toast\.makeText\(this, "Anti-Bloqueio DESATIVADO", Toast\.LENGTH_SHORT\)\.show\(\)\s*\}\s*\}', '', kt, flags=re.DOTALL)

with open('app/src/main/java/com/iptv/app/SettingsActivity.kt', 'w', encoding='utf-8') as f:
    f.write(kt)

print("Done")
