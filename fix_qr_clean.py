# -*- coding: utf-8 -*-
import re

with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'r', encoding='utf-8') as f:
    kt = f.read()

# Strip any existing showQrDialog function definitions:
kt = re.sub(r'\n\s*private fun showQrDialog\(\) \{.*?\n\s*\}', '', kt, flags=re.DOTALL)
# One more pass to be sure in case of nested braces (though there are no nested braces in showQrDialog)
kt = re.sub(r'private fun showQrDialog\(\) \{.*?dialog\.show\(\)\n\s*\}', '', kt, flags=re.DOTALL)

func = '''
    private fun showQrDialog() {
        val dialog = android.app.Dialog(this@MainActivity)
        dialog.setContentView(R.layout.dialog_qr)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val ivQr = dialog.findViewById<android.widget.ImageView>(R.id.ivQrCode)
        val apkUrl = "https://tinyurl.com/2985xryp"
        val qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=400x400&data=" + android.net.Uri.encode(apkUrl)
        
        com.bumptech.glide.Glide.with(this@MainActivity).load(qrUrl).into(ivQr)
        dialog.show()
    }
'''

# Find the last closing brace of the file
last_brace_index = kt.rfind('}')
if last_brace_index != -1:
    kt = kt[:last_brace_index] + func + '\n}' + kt[last_brace_index+1:]

with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(kt)

print("Cleaned and fixed")
