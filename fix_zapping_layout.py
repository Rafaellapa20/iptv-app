# -*- coding: utf-8 -*-
with open('app/src/main/res/layout/activity_player.xml', 'r', encoding='utf-8') as f:
    text = f.read()

new_tv = '''
    <!-- Zapping Numérico Overlay -->
    <TextView
        android:id="@+id/tvChannelNumber"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="top|end"
        android:layout_marginTop="32dp"
        android:layout_marginEnd="48dp"
        android:textColor="#FFFFFF"
        android:textSize="50sp"
        android:textStyle="bold"
        android:shadowColor="#000000"
        android:shadowDx="2"
        android:shadowDy="2"
        android:shadowRadius="4"
        android:visibility="gone" />
'''

if 'tvChannelNumber' not in text:
    text = text.replace('</FrameLayout>', new_tv + '\n</FrameLayout>')

with open('app/src/main/res/layout/activity_player.xml', 'w', encoding='utf-8') as f:
    f.write(text)
print("Done")
