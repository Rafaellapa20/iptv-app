# -*- coding: utf-8 -*-
import re

# Update LoginActivity
with open('app/src/main/java/com/iptv/app/LoginActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()
search = r'val intent = Intent\(this@LoginActivity, MainActivity::class\.java\)'
replace = '''val target = if (DeviceUtils.isTv(this@LoginActivity)) MainActivity::class.java else MobileMainActivity::class.java
                    val intent = Intent(this@LoginActivity, target)'''
text = re.sub(search, replace, text)
with open('app/src/main/java/com/iptv/app/LoginActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)

# Update UsersActivity
with open('app/src/main/java/com/iptv/app/UsersActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()
search = r'val intent = Intent\(this@UsersActivity, MainActivity::class\.java\)'
replace = '''val target = if (DeviceUtils.isTv(this@UsersActivity)) MainActivity::class.java else MobileMainActivity::class.java
                    val intent = Intent(this@UsersActivity, target)'''
text = re.sub(search, replace, text)
with open('app/src/main/java/com/iptv/app/UsersActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)

# Update AndroidManifest.xml
with open('app/src/main/AndroidManifest.xml', 'r', encoding='utf-8') as f:
    text = f.read()

# Make sure we don't force landscape on all screens globally
search_manifest = r'<activity\s*android:name="\.MobileMainActivity".*?/>'
if 'MobileMainActivity' not in text:
    search_manifest = r'</application>'
    replace_manifest = '''    <activity
            android:name=".MobileMainActivity"
            android:exported="false"
            android:screenOrientation="portrait"
            android:theme="@style/AppTheme" />
    </application>'''
    text = text.replace(search_manifest, replace_manifest)
    with open('app/src/main/AndroidManifest.xml', 'w', encoding='utf-8') as f:
        f.write(text)

print("Done")
