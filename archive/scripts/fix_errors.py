# -*- coding: utf-8 -*-

# Fix Radios button
with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

search = 'findViewById<View>(R.id.btnQuickEpg)?.setOnClickListener {'
replace = '''findViewById<View>(R.id.btnQuickRadios)?.setOnClickListener {
            startActivity(android.content.Intent(this, RadiosActivity::class.java))
        }
        findViewById<View>(R.id.btnQuickEpg)?.setOnClickListener {'''
text = text.replace(search, replace)
with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)

# Fix bg_button in activity_player.xml
with open('app/src/main/res/layout/activity_player.xml', 'r', encoding='utf-8') as f:
    xml_text = f.read()

xml_text = xml_text.replace('@drawable/bg_button', '#333333')

with open('app/src/main/res/layout/activity_player.xml', 'w', encoding='utf-8') as f:
    f.write(xml_text)

print("Done")
