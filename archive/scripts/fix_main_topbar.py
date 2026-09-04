# -*- coding: utf-8 -*-
with open('app/src/main/res/layout/activity_main.xml', 'r', encoding='utf-8') as f:
    text = f.read()

import re

search = r'<!-- HERO ZONE \(LIVE TV, MOVIES, SERIES\) -->'
replace = '''<!-- MENU SUPERIOR (LIVE TV | FILMES | SERIES) PARA FACILITAR NAVEGAÇÃO -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center"
        android:paddingTop="12dp"
        android:paddingBottom="12dp">
        
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="LIVE TV"
            android:textColor="#FFFFFF"
            android:textSize="18sp"
            android:textStyle="bold"
            android:paddingHorizontal="24dp" />
            
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="|"
            android:textColor="#555555"
            android:textSize="18sp" />
            
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="FILMES"
            android:textColor="#FFFFFF"
            android:textSize="18sp"
            android:textStyle="bold"
            android:paddingHorizontal="24dp" />
            
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="|"
            android:textColor="#555555"
            android:textSize="18sp" />
            
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="SÉRIES"
            android:textColor="#FFFFFF"
            android:textSize="18sp"
            android:textStyle="bold"
            android:paddingHorizontal="24dp" />
    </LinearLayout>

    <!-- HERO ZONE (LIVE TV, MOVIES, SERIES) -->'''

text = text.replace(search, replace)

with open('app/src/main/res/layout/activity_main.xml', 'w', encoding='utf-8') as f:
    f.write(text)

print("Done")
