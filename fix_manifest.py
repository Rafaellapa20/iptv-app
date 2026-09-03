# -*- coding: utf-8 -*-
import re

with open('app/src/main/AndroidManifest.xml', 'r', encoding='utf-8') as f:
    text = f.read()

search = r'<activity\s*android:name="\.MovieInfoActivity"[^>]*>'
replace = '''<activity
            android:name=".MovieInfoActivity"
            android:exported="false"
            android:screenOrientation="landscape"
            android:theme="@style/AppTheme" />
        <activity
            android:name=".SeriesInfoActivity"
            android:exported="false"
            android:screenOrientation="landscape"
            android:theme="@style/AppTheme" />'''

text = re.sub(search, replace, text)

with open('app/src/main/AndroidManifest.xml', 'w', encoding='utf-8') as f:
    f.write(text)

print("Done")
