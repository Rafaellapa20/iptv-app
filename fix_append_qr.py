# -*- coding: utf-8 -*-
with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'r', encoding='utf-8') as f:
    kt = f.read()

func = '''
    private fun showQrDialog() {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_qr)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val ivQr = dialog.findViewById<android.widget.ImageView>(R.id.ivQrCode)
        val apkUrl = "https://tinyurl.com/2985xryp"
        val qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=400x400&data=" + android.net.Uri.encode(apkUrl)
        
        com.bumptech.glide.Glide.with(this).load(qrUrl).into(ivQr)
        dialog.show()
    }
'''

# Find the last brace
parts = kt.rsplit('}', 1)
kt = parts[0] + func + '\n}'

with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(kt)

print("Added showQrDialog safely")
