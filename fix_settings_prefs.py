# -*- coding: utf-8 -*-
with open('app/src/main/java/com/iptv/app/SettingsActivity.kt', 'r', encoding='utf-8') as f:
    kt = f.read()

# Replace the first al prefs = getSharedPreferences... with an update to the second one, or just rename the first one
kt = kt.replace('val prefs = getSharedPreferences("IPTV_PREFS", MODE_PRIVATE)\n        val expDate = prefs.getString("EXP_DATE", "Ilimitado") ?: "Ilimitado"',
                'val prefsExp = getSharedPreferences("IPTV_PREFS", MODE_PRIVATE)\n        val expDate = prefsExp.getString("EXP_DATE", "Ilimitado") ?: "Ilimitado"')

with open('app/src/main/java/com/iptv/app/SettingsActivity.kt', 'w', encoding='utf-8') as f:
    f.write(kt)

print("SettingsActivity fixed")
