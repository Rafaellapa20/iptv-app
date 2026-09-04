# -*- coding: utf-8 -*-
with open('app/src/main/res/layout/activity_main.xml', 'r', encoding='utf-8') as f:
    text = f.read()

import re

# Add button to llQuickAccessBar
new_button = '''
        <Button
            android:id="@+id/btnQuickRadios"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:layout_marginEnd="6dp"
            android:background="@drawable/bg_smarters_sage"
            android:text="📻 Rádios"
            android:textColor="#FFFFFF"
            android:textSize="11sp"
            android:textStyle="bold"
            android:textAllCaps="false"
            android:focusable="true"
            android:clickable="true"
            android:nextFocusUp="@id/cardSeries"
            android:nextFocusDown="@id/rvContinueWatching"
            android:nextFocusLeft="@id/btnQuickSettings"
            android:nextFocusRight="@id/btnQuickCatchup" />
'''

# We need to insert this before btnQuickCatchup and update nextFocus
text = text.replace('android:nextFocusRight="@id/btnQuickCatchup" />', 'android:nextFocusRight="@id/btnQuickRadios" />')
text = text.replace('android:nextFocusLeft="@id/btnQuickSettings" />', 'android:nextFocusLeft="@id/btnQuickRadios" />')

text = text.replace('<Button\n            android:id="@+id/btnQuickCatchup"', new_button + '\n        <Button\n            android:id="@+id/btnQuickCatchup"')

with open('app/src/main/res/layout/activity_main.xml', 'w', encoding='utf-8') as f:
    f.write(text)

with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'r', encoding='utf-8') as f:
    text_kt = f.read()

text_kt = text_kt.replace(
    'findViewById<View>(R.id.btnQuickCatchup)?.setOnClickListener { Toast.makeText(this, "Definições...", Toast.LENGTH_SHORT).show() }',
    'findViewById<View>(R.id.btnQuickCatchup)?.setOnClickListener { Toast.makeText(this, "Definições...", Toast.LENGTH_SHORT).show() }\n        findViewById<View>(R.id.btnQuickRadios)?.setOnClickListener { startActivity(Intent(this, RadiosActivity::class.java)) }'
)

with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text_kt)

with open('app/src/main/AndroidManifest.xml', 'r', encoding='utf-8') as f:
    text_man = f.read()

text_man = text_man.replace('</application>', '    <activity android:name=".RadiosActivity" android:exported="false" android:screenOrientation="landscape" />\n    </application>')

with open('app/src/main/AndroidManifest.xml', 'w', encoding='utf-8') as f:
    f.write(text_man)

print("Done")
