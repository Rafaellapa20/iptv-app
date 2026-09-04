# -*- coding: utf-8 -*-
with open('app/src/main/res/layout/activity_settings.xml', 'r', encoding='utf-8') as f:
    xml = f.read()

new_tv = '''
                <TextView
                    android:id="@+id/tvValidade"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:textAlignment="center"
                    android:text="A verificar validade..."
                    android:textColor="#00FF00"
                    android:textSize="14sp"
                    android:layout_marginBottom="10dp"/>
'''
xml = xml.replace('android:id="@+id/tvAppVersion"', 'android:id="@+id/tvValidade"\n                    android:layout_width="match_parent"\n                    android:layout_height="wrap_content"\n                    android:textAlignment="center"\n                    android:text="Validade: Ilimitado"\n                    android:textColor="#00FF00"\n                    android:textSize="14sp"\n                    android:layout_marginBottom="10dp"/>\n                <TextView\n                    android:id="@+id/tvAppVersion"')

with open('app/src/main/res/layout/activity_settings.xml', 'w', encoding='utf-8') as f:
    f.write(xml)
