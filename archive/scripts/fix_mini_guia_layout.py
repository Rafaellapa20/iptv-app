# -*- coding: utf-8 -*-
with open('app/src/main/res/layout/activity_player.xml', 'r', encoding='utf-8') as f:
    text = f.read()

mini_guia_xml = '''
    <!-- Mini Guia Zapping Transparente -->
    <LinearLayout
        android:id="@+id/llMiniGuia"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom"
        android:background="#99000000"
        android:orientation="vertical"
        android:padding="16dp"
        android:visibility="gone">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Zapping"
            android:textColor="#00E5FF"
            android:textSize="14sp"
            android:textStyle="bold"
            android:layout_marginBottom="8dp" />

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/rvMiniGuia"
            android:layout_width="match_parent"
            android:layout_height="90dp"
            android:orientation="horizontal" />
    </LinearLayout>
'''

if 'llMiniGuia' not in text:
    text = text.replace('</FrameLayout>', mini_guia_xml + '\n</FrameLayout>')

with open('app/src/main/res/layout/activity_player.xml', 'w', encoding='utf-8') as f:
    f.write(text)
print("Done")
