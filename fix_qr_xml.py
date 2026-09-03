# -*- coding: utf-8 -*-
with open('app/src/main/res/layout/activity_main.xml', 'r', encoding='utf-8') as f:
    xml = f.read()

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

xml = xml.replace('</LinearLayout>', qr_btn + '\n</LinearLayout>')

with open('app/src/main/res/layout/activity_main.xml', 'w', encoding='utf-8') as f:
    f.write(xml)

print("Done")
