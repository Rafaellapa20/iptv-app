# -*- coding: utf-8 -*-
import re

with open('app/src/main/res/layout/activity_main.xml', 'r', encoding='utf-8') as f:
    xml = f.read()

# The button string that was injected multiple times
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

# Remove ALL instances of it
xml = xml.replace(qr_btn, '')

# Now insert it EXACTLY at the end of the root layout.
# The root layout of activity_main.xml is probably a RelativeLayout or LinearLayout.
# Let's just find the very last </ tag in the document.
last_tag_pos = xml.rfind('</')
if last_tag_pos != -1:
    xml = xml[:last_tag_pos] + qr_btn + '\n' + xml[last_tag_pos:]

with open('app/src/main/res/layout/activity_main.xml', 'w', encoding='utf-8') as f:
    f.write(xml)

print("Done")
