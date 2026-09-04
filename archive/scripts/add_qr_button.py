# -*- coding: utf-8 -*-
import re

# 1. Update activity_main.xml to add a button
with open('app/src/main/res/layout/activity_main.xml', 'r', encoding='utf-8') as f:
    xml = f.read()

# Add a floating action button or just a button at the top or bottom.
# Let's add it to the top right corner.
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
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_marginTop="16dp"
        android:layout_marginEnd="16dp" />
'''

xml = xml.replace('</androidx.constraintlayout.widget.ConstraintLayout>', qr_btn + '\n</androidx.constraintlayout.widget.ConstraintLayout>')

with open('app/src/main/res/layout/activity_main.xml', 'w', encoding='utf-8') as f:
    f.write(xml)

# 2. Update MainActivity.kt to show the dialog
with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'r', encoding='utf-8') as f:
    kt = f.read()

qr_logic = '''
        findViewById<View>(R.id.btnQrCode)?.setOnClickListener {
            showQrDialog()
        }
'''

# Find the end of onCreate
kt = re.sub(r'(RemoteManager\.startTvServer\(this, username, password\))', r'\1' + qr_logic, kt)

dialog_method = '''
    private fun showQrDialog() {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_qr)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val ivQr = dialog.findViewById<ImageView>(R.id.ivQrCode)
        val apkUrl = "https://github.com/Rafaellapa20/iptv-app/raw/main/iptv_v10.36-debug.apk"
        val qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=400x400&data=" + Uri.encode(apkUrl)
        
        Glide.with(this).load(qrUrl).into(ivQr)
        dialog.show()
    }
'''

# Insert the method before the last brace
kt = kt[:kt.rfind('}')] + dialog_method + '\n}'

with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(kt)

print("Done")
