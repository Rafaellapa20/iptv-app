# -*- coding: utf-8 -*-
import re
with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'r', encoding='utf-8') as f:
    kt = f.read()

# The bad replace was:
# kt = kt.replace('}', '}\n' + func)
# So every '}' is followed by '\n    private fun showQrDialog() { ... dialog.show()\n    }'

pattern = r'\n    private fun showQrDialog\(\) \{\n        val dialog = android\.app\.Dialog\(this\)\n        dialog\.setContentView\(R\.layout\.dialog_qr\)\n        dialog\.window\?\.setBackgroundDrawableResource\(android\.R\.color\.transparent\)\n        \n        val ivQr = dialog\.findViewById<android\.widget\.ImageView>\(R\.id\.ivQrCode\)\n        val apkUrl = "https://tinyurl.com/2985xryp"\n        val qrUrl = "https://api\.qrserver\.com/v1/create-qr-code/\?size=400x400&data=" \+ android\.net\.Uri\.encode\(apkUrl\)\n        \n        com\.bumptech\.glide\.Glide\.with\(this\)\.load\(qrUrl\)\.into\(ivQr\)\n        dialog\.show\(\)\n    \}'

# The func text from the script:
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

# Remove all literal occurrences of func
kt = kt.replace('\n' + func, '')
kt = kt.replace(func, '')

# Wait, there was also a broken replace because I did parts = kt.rsplit('}', 1).
# Just to be safe, I will just clean up everything that looks like it.

kt = re.sub(r'\n    private fun showQrDialog\(\) \{.*?\n    \}', '', kt, flags=re.DOTALL)

with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(kt)

print("Duplicates removed")
