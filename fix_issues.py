# -*- coding: utf-8 -*-

# Fix Radios Button
with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'r', encoding='utf-8') as f:
    main_text = f.read()

if 'btnQuickRadios' not in main_text:
    main_text = main_text.replace('val btnQuickEpg = findViewById<View>(R.id.btnQuickEpg)', 'val btnQuickEpg = findViewById<View>(R.id.btnQuickEpg)\n        val btnQuickRadios = findViewById<View>(R.id.btnQuickRadios)\n        btnQuickRadios.setOnClickListener { startActivity(Intent(this, RadiosActivity::class.java)) }')
    with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'w', encoding='utf-8') as f:
        f.write(main_text)

# Fix SpeedTest duration
with open('app/src/main/java/com/iptv/app/SpeedTestActivity.kt', 'r', encoding='utf-8') as f:
    speed_text = f.read()

speed_text = speed_text.replace('val testDurationMs = 8000L', 'val testDurationMs = 3500L')
with open('app/src/main/java/com/iptv/app/SpeedTestActivity.kt', 'w', encoding='utf-8') as f:
    f.write(speed_text)

# Fix PiP on Back Press in PlayerActivity
with open('app/src/main/java/com/iptv/app/PlayerActivity.kt', 'r', encoding='utf-8') as f:
    player_text = f.read()

pip_code = '''
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        val type = intent.getStringExtra("TYPE")
        if (type == "live" && packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            val params = android.app.PictureInPictureParams.Builder()
                .setAspectRatio(android.util.Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        } else {
            super.onBackPressed()
        }
    }
'''

if 'override fun onBackPressed()' in player_text:
    player_text = player_text.replace('override fun onBackPressed() {', pip_code + '\n    // replaced: override fun onBackPressed() {')
else:
    player_text = player_text.replace('private fun hideMiniGuia()', pip_code + '\n    private fun hideMiniGuia()')

with open('app/src/main/java/com/iptv/app/PlayerActivity.kt', 'w', encoding='utf-8') as f:
    f.write(player_text)

print("Done")
