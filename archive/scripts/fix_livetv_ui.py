# -*- coding: utf-8 -*-
with open('app/src/main/res/layout/activity_live_tv.xml', 'r', encoding='utf-8') as f:
    text = f.read()

import re

search = r'<!-- Coluna 3: Preview e EPG -->.*?</androidx.constraintlayout.widget.ConstraintLayout>'

replace = '''<!-- Coluna 3: Preview e EPG -->
    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:orientation="vertical"
        android:padding="24dp"
        android:background="@drawable/bg_epg_panel"
        app:layout_constraintStart_toEndOf="@id/rvChannels"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintTop_toBottomOf="@id/topBar"
        app:layout_constraintBottom_toBottomOf="parent">

        <androidx.cardview.widget.CardView
            android:layout_width="match_parent"
            android:layout_height="220dp"
            app:cardCornerRadius="16dp"
            app:cardElevation="8dp"
            app:cardBackgroundColor="#000000">

            <androidx.media3.ui.PlayerView
                android:id="@+id/mini_player_view"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:background="#000000"
                app:use_controller="false"
                app:resize_mode="fill"/>
        </androidx.cardview.widget.CardView>

        <TextView
            android:id="@+id/tvPreviewName"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Nenhum canal selecionado"
            android:textColor="#FFFFFF"
            android:textSize="22sp"
            android:textStyle="bold"
            android:layout_marginTop="24dp"/>

        <androidx.cardview.widget.CardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            app:cardCornerRadius="12dp"
            app:cardElevation="4dp"
            app:cardBackgroundColor="#1AFFFFFF">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="16dp">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="A DAR AGORA"
                    android:textColor="#00E5FF"
                    android:textSize="12sp"
                    android:textStyle="bold"
                    android:letterSpacing="0.1"/>

                <TextView
                    android:id="@+id/tvEpgCurrent"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:textColor="#FFFFFF"
                    android:textSize="16sp"
                    android:layout_marginTop="8dp"
                    android:lineSpacingExtra="4dp" />

                <ProgressBar
                    android:id="@+id/pbEpgProgress"
                    style="?android:attr/progressBarStyleHorizontal"
                    android:layout_width="match_parent"
                    android:layout_height="6dp"
                    android:layout_marginTop="12dp"
                    android:progressDrawable="@drawable/bg_progress"
                    android:visibility="gone"/>
            </LinearLayout>
        </androidx.cardview.widget.CardView>

    </LinearLayout>

    <ProgressBar
        android:id="@+id/progressBar"
        android:layout_width="64dp"
        android:layout_height="64dp"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:visibility="gone"/>

</androidx.constraintlayout.widget.ConstraintLayout>
'''

text = re.sub(search, replace, text, flags=re.DOTALL)

with open('app/src/main/res/layout/activity_live_tv.xml', 'w', encoding='utf-8') as f:
    f.write(text)
print("Done")
