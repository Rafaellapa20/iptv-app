# -*- coding: utf-8 -*-
with open('app/src/main/res/layout/activity_player.xml', 'r', encoding='utf-8') as f:
    text = f.read()

new_tv = '''        <TextView
            android:id="@+id/tvDiagnostics"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_below="@id/tvLoadingTitle"
            android:layout_centerHorizontal="true"
            android:layout_marginTop="8dp"
            android:text=""
            android:textColor="#FFCC00"
            android:textSize="12sp"
            android:visibility="gone"
            android:textStyle="italic" />'''

text = text.replace('android:textStyle="bold" />', 'android:textStyle="bold" />\n' + new_tv)

with open('app/src/main/res/layout/activity_player.xml', 'w', encoding='utf-8') as f:
    f.write(text)
print("Done")
