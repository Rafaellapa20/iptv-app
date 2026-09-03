# -*- coding: utf-8 -*-
with open('app/src/main/java/com/iptv/app/SettingsActivity.kt', 'r', encoding='utf-8') as f:
    kt = f.read()

kt = kt.replace('val btnLogout = findViewById<Button>(R.id.btnLogout)', 'val prefs = getSharedPreferences("IPTV_PREFS", MODE_PRIVATE)\n        val btnLogout = findViewById<Button>(R.id.btnLogout)')

with open('app/src/main/java/com/iptv/app/SettingsActivity.kt', 'w', encoding='utf-8') as f:
    f.write(kt)

print("Done")
