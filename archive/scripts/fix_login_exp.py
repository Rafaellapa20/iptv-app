# -*- coding: utf-8 -*-
with open('app/src/main/java/com/iptv/app/LoginActivity.kt', 'r', encoding='utf-8') as f:
    kt = f.read()

kt = kt.replace('.putString("PASSWORD", password)', '.putString("PASSWORD", password)\n                            .putString("EXP_DATE", expDateFormated)')

with open('app/src/main/java/com/iptv/app/LoginActivity.kt', 'w', encoding='utf-8') as f:
    f.write(kt)

print("Done")
