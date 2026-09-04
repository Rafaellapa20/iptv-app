# -*- coding: utf-8 -*-
with open('app/src/main/res/layout/activity_main.xml', 'r', encoding='utf-8') as f:
    xml = f.read()

import re

# Remove the floating QR button
qr_btn = '''
    <Button
        android:id="@+id/btnQrCode"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="📱 App Telemóvel (QR)"
        android:textColor="#FFFFFF"
        android:background="@drawable/bg_smarters_sage"
        android:textSize="12sp"
        android:padding="8dp"
        android:layout_gravity="end|top"
        android:layout_marginTop="16dp"
        android:layout_marginEnd="16dp" />
'''
xml = xml.replace(qr_btn, '')
xml = xml.replace(qr_btn.strip(), '')

# Replace Radios button text and ID
search = r'<Button\s*android:id="@+id/btnQuickRadios"[\s\S]*?android:text="[^"]*"[\s\S]*?/>'
replace = '''<Button
            android:id="@+id/btnQrCode"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:layout_marginEnd="6dp"
            android:background="@drawable/bg_smarters_sage"
            android:text="📱 Telemóvel"
            android:textColor="#FFFFFF"
            android:textSize="11sp"
            android:textStyle="bold"
            android:textAllCaps="false"
            android:focusable="true"
            android:clickable="true"
            android:nextFocusUp="@id/cardSeries"
            android:nextFocusDown="@id/rvContinueWatching"
            android:nextFocusLeft="@id/btnQuickSettings"
            android:nextFocusRight="@id/btnQuickCatchup" />'''

xml = re.sub(search, replace, xml)

with open('app/src/main/res/layout/activity_main.xml', 'w', encoding='utf-8') as f:
    f.write(xml)

print("Done")
