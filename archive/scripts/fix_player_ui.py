# -*- coding: utf-8 -*-
with open('app/src/main/res/layout/activity_player.xml', 'r', encoding='utf-8') as f:
    text = f.read()

import re

search = r'<!-- Overlay EPG Info Bar \(Bottom\) -->.*?</LinearLayout>'

replace = '''<!-- Overlay EPG Info Bar (Bottom) -->
    <androidx.cardview.widget.CardView
        android:id="@+id/epgContainer"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom"
        android:layout_margin="24dp"
        app:cardCornerRadius="16dp"
        app:cardElevation="12dp"
        app:cardBackgroundColor="#E60F172A"
        android:visibility="gone">
        
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="20dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:gravity="center_vertical">

                <TextView
                    android:id="@+id/tvEpgProgram"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:textColor="#FFFFFF"
                    android:textSize="18sp"
                    android:textStyle="bold"
                    android:lineSpacingExtra="4dp"
                    android:text="Sem Informa\u00e7\u00e3o de EPG" />

                <!-- Network Status Indicator -->
                <LinearLayout
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:gravity="center_vertical"
                    android:layout_marginStart="16dp"
                    android:background="@drawable/bg_button"
                    android:paddingHorizontal="12dp"
                    android:paddingVertical="6dp">

                    <ImageView
                        android:id="@+id/ivNetworkStatus"
                        android:layout_width="12dp"
                        android:layout_height="12dp"
                        android:src="@android:drawable/presence_online"
                        app:tint="#00E5FF" />

                    <TextView
                        android:id="@+id/tvNetworkSpeed"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginStart="8dp"
                        android:textColor="#00E5FF"
                        android:textSize="12sp"
                        android:textStyle="bold"
                        android:text="--- Mbps" />
                </LinearLayout>
            </LinearLayout>

            <TextView
                android:id="@+id/tvEpgTime"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="8dp"
                android:textColor="#94A3B8"
                android:textSize="14sp"
                android:text="--:-- - --:--" />

            <ProgressBar
                android:id="@+id/pbEpgProgress"
                style="?android:attr/progressBarStyleHorizontal"
                android:layout_width="match_parent"
                android:layout_height="6dp"
                android:layout_marginTop="12dp"
                android:progressDrawable="@drawable/bg_progress"
                android:visibility="gone" />
        </LinearLayout>
    </androidx.cardview.widget.CardView>'''

# Need to replace the FIRST occurrence of the search block
text = re.sub(search, replace, text, count=1, flags=re.DOTALL)

with open('app/src/main/res/layout/activity_player.xml', 'w', encoding='utf-8') as f:
    f.write(text)
print("Done")
