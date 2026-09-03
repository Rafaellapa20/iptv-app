# -*- coding: utf-8 -*-
with open('app/src/main/res/layout/activity_main.xml', 'r', encoding='utf-8') as f:
    xml = f.read()

import re
# Replace Catchup button with Senior button
search = r'<Button\s*android:id="@+id/btnQuickCatchup"[\s\S]*?android:text="[^"]*"[\s\S]*?/>'
replace = '''<Button
            android:id="@+id/btnQuickSenior"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:background="#2E7D32"
            android:text="👴 Modo Idosos"
            android:textColor="#FFFFFF"
            android:textSize="11sp"
            android:textStyle="bold"
            android:textAllCaps="false"
            android:focusable="true"
            android:clickable="true"
            android:nextFocusUp="@id/cardSeries"
            android:nextFocusDown="@id/rvContinueWatching"
            android:nextFocusLeft="@id/btnQrCode" />'''

xml = re.sub(search, replace, xml)

with open('app/src/main/res/layout/activity_main.xml', 'w', encoding='utf-8') as f:
    f.write(xml)

# Also update MainActivity.kt to route it
with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'r', encoding='utf-8') as f:
    kt = f.read()

senior_logic = '''
        findViewById<View>(R.id.btnQuickSenior)?.setOnClickListener {
            val p = getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
            p.edit().putBoolean("is_senior_mode", true).apply()
            val intent = Intent(this, SeniorMainActivity::class.java)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
            finish()
        }
'''

# insert it where the other clicks are
kt = kt.replace('findViewById<View>(R.id.btnQrCode)?.setOnClickListener {', senior_logic + '\n        findViewById<View>(R.id.btnQrCode)?.setOnClickListener {')

with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(kt)

print("Done")
