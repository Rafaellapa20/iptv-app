# -*- coding: utf-8 -*-
with open('app/src/main/java/com/iptv/app/LoginActivity.kt', 'r', encoding='utf-8') as f:
    kt = f.read()

search = r'if \(DeviceUtils\.isTv\(this\)\) \{\s*val intent = Intent\(this, MainActivity::class\.java\)'
replace = '''if (DeviceUtils.isTv(this)) {
                            val p = getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
                            val isSenior = p.getBoolean("is_senior_mode", false)
                            val intent = if (isSenior) {
                                Intent(this, SeniorMainActivity::class.java)
                            } else {
                                Intent(this, MainActivity::class.java)
                            }'''

kt = kt.replace('if (DeviceUtils.isTv(this)) {\n                                val intent = Intent(this, MainActivity::class.java)', replace)

with open('app/src/main/java/com/iptv/app/LoginActivity.kt', 'w', encoding='utf-8') as f:
    f.write(kt)

print("Done")
