# -*- coding: utf-8 -*-
with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'r', encoding='utf-8') as f:
    kt = f.read()

kt = kt.replace(
    'findViewById<View>(R.id.btnQuickSettings)?.setOnClickListener { startActivity(Intent(this, SpeedTestActivity::class.java)) }',
    '''findViewById<View>(R.id.btnQuickSettings)?.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        findViewById<View>(R.id.btnQuickRadios)?.setOnClickListener { showQrDialog() }'''
)

with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(kt)

print("Buttons fixed")
