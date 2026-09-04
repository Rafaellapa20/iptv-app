# -*- coding: utf-8 -*-

import re

# 1. Fix onPause in PlayerActivity.kt
with open('app/src/main/java/com/iptv/app/PlayerActivity.kt', 'r', encoding='utf-8') as f:
    player_text = f.read()

onpause_search = '''    override fun onPause() {
        super.onPause()
        saveCurrentProgress()
        player1?.pause()
        player2?.pause()
    }'''

onpause_replace = '''    override fun onPause() {
        super.onPause()
        saveCurrentProgress()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N && isInPictureInPictureMode) {
            // Continuar a dar em PiP
        } else {
            player1?.pause()
            player2?.pause()
        }
    }'''

player_text = player_text.replace(onpause_search, onpause_replace)

# 2. Fix Zapping layout logic (Vertical)
zapping_logic_search = '''                    if (intent.getStringExtra("TYPE") == "live") {
                        if (!isMiniGuiaVisible) {
                            showMiniGuia()
                        } else {
                            hideMiniGuia()
                        }
                    } else {
                        changeChannel(event.keyCode == KeyEvent.KEYCODE_DPAD_DOWN)
                    }
                    return true'''

zapping_logic_replace = '''                    if (intent.getStringExtra("TYPE") == "live") {
                        if (!isMiniGuiaVisible) {
                            showMiniGuia()
                            return true
                        }
                        // Se j\u00e1 estiver vis\u00edvel, deixar o RecyclerView (setas) navegar normalmente
                    } else {
                        changeChannel(event.keyCode == KeyEvent.KEYCODE_DPAD_DOWN)
                        return true
                    }'''
player_text = player_text.replace(zapping_logic_search, zapping_logic_replace)

# Make RecyclerView vertical
vertical_search = 'rvMiniGuia.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this, androidx.recyclerview.widget.RecyclerView.HORIZONTAL, false)'
vertical_replace = 'rvMiniGuia.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this, androidx.recyclerview.widget.RecyclerView.VERTICAL, false)'
player_text = player_text.replace(vertical_search, vertical_replace)

with open('app/src/main/java/com/iptv/app/PlayerActivity.kt', 'w', encoding='utf-8') as f:
    f.write(player_text)

# 3. Change XML layout to be vertical on the left
with open('app/src/main/res/layout/activity_player.xml', 'r', encoding='utf-8') as f:
    xml_text = f.read()

xml_search = '''    <!-- Mini Guia Zapping Transparente -->
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
    </LinearLayout>'''

xml_replace = '''    <!-- Mini Guia Zapping Transparente -->
    <LinearLayout
        android:id="@+id/llMiniGuia"
        android:layout_width="250dp"
        android:layout_height="match_parent"
        android:layout_gravity="start"
        android:background="#E6000000"
        android:orientation="vertical"
        android:padding="16dp"
        android:visibility="gone">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="CANAIS"
            android:textColor="#00E5FF"
            android:textSize="18sp"
            android:textStyle="bold"
            android:gravity="center"
            android:layout_marginBottom="16dp" />

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/rvMiniGuia"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:orientation="vertical" />
    </LinearLayout>'''

xml_text = xml_text.replace(xml_search, xml_replace)

with open('app/src/main/res/layout/activity_player.xml', 'w', encoding='utf-8') as f:
    f.write(xml_text)

# Fix item_mini_guia height
with open('app/src/main/res/layout/item_mini_guia.xml', 'r', encoding='utf-8') as f:
    item_xml = f.read()

item_xml = item_xml.replace('android:layout_width="120dp"', 'android:layout_width="match_parent"')
item_xml = item_xml.replace('android:layout_height="80dp"', 'android:layout_height="60dp"')
item_xml = item_xml.replace('android:layout_marginEnd="10dp"', 'android:layout_marginBottom="10dp"')
item_xml = item_xml.replace('android:orientation="vertical"', 'android:orientation="horizontal"')
item_xml = item_xml.replace('android:gravity="center"', 'android:gravity="center_vertical"')

# Also make the image smaller and text to the right
item_img_search = '''    <ImageView
        android:id="@+id/ivMiniLogo"
        android:layout_width="40dp"
        android:layout_height="40dp"
        android:layout_marginBottom="4dp"
        android:scaleType="fitCenter" />'''

item_img_replace = '''    <ImageView
        android:id="@+id/ivMiniLogo"
        android:layout_width="30dp"
        android:layout_height="30dp"
        android:layout_marginEnd="8dp"
        android:scaleType="fitCenter" />'''
item_xml = item_xml.replace(item_img_search, item_img_replace)

item_text_search = '''        android:gravity="center"
        android:lines="1"'''

item_text_replace = '''        android:gravity="start|center_vertical"
        android:lines="2"'''
item_xml = item_xml.replace(item_text_search, item_text_replace)

with open('app/src/main/res/layout/item_mini_guia.xml', 'w', encoding='utf-8') as f:
    f.write(item_xml)

print("Done")
