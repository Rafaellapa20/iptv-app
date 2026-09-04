# -*- coding: utf-8 -*-
with open('app/src/main/res/layout/activity_settings.xml', 'r', encoding='utf-8') as f:
    xml = f.read()

# I will just add the TextView to the settings xml inside the top LinearLayout
new_tv = '''
    <TextView
        android:id="@+id/tvValidade"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:text="A verificar validade..."
        android:textColor="#00FF00"
        android:textSize="14sp"
        android:layout_marginBottom="10dp"/>
'''
xml = xml.replace('<TextView\n        android:id="@+id/tvAppVersion"', new_tv + '\n    <TextView\n        android:id="@+id/tvAppVersion"')

with open('app/src/main/res/layout/activity_settings.xml', 'w', encoding='utf-8') as f:
    f.write(xml)

with open('app/src/main/java/com/iptv/app/SettingsActivity.kt', 'r', encoding='utf-8') as f:
    kt = f.read()

# In SettingsActivity, we fetch the expiration from shared prefs.
logic = '''
        val prefs = getSharedPreferences("IPTV_PREFS", MODE_PRIVATE)
        val expDate = prefs.getString("EXP_DATE", "Ilimitado") ?: "Ilimitado"
        val tvValidade = findViewById<TextView>(R.id.tvValidade)
        tvValidade.text = "Validade da Conta: " + expDate
'''

kt = kt.replace('val tvAppVersion = findViewById<TextView>(R.id.tvAppVersion)', logic + '\n        val tvAppVersion = findViewById<TextView>(R.id.tvAppVersion)')

with open('app/src/main/java/com/iptv/app/SettingsActivity.kt', 'w', encoding='utf-8') as f:
    f.write(kt)

print("Done")
