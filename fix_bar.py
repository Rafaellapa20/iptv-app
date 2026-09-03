# -*- coding: utf-8 -*-
import re

with open('app/src/main/res/layout/activity_main.xml', 'r', encoding='utf-8') as f:
    xml = f.read()

new_bar = '''
    <LinearLayout
        android:id="@+id/llQuickAccessBar"
        android:layout_width="match_parent"
        android:layout_height="40dp"
        android:orientation="horizontal"
        android:paddingHorizontal="20dp"
        android:layout_marginBottom="6dp"
        android:weightSum="5"
        android:descendantFocusability="afterDescendants"
        android:focusable="false">

        <Button
            android:id="@+id/btnQuickFavorites"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:layout_marginEnd="6dp"
            android:background="@drawable/bg_smarters_sage"
            android:text="⭐ Favoritos"
            android:textColor="#FFFFFF"
            android:textSize="11sp"
            android:nextFocusUp="@id/cardTv"
            android:nextFocusDown="@id/rvContinueWatching"
            android:nextFocusRight="@id/btnQuickEpg" />

        <Button
            android:id="@+id/btnQuickEpg"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:layout_marginEnd="6dp"
            android:background="@drawable/bg_smarters_sage"
            android:text="📺 Guia EPG"
            android:textColor="#FFFFFF"
            android:textSize="11sp"
            android:nextFocusUp="@id/cardFilmes"
            android:nextFocusDown="@id/rvContinueWatching"
            android:nextFocusLeft="@id/btnQuickFavorites"
            android:nextFocusRight="@id/btnQuickSettings" />

        <Button
            android:id="@+id/btnQuickSettings"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:layout_marginEnd="6dp"
            android:background="@drawable/bg_smarters_sage"
            android:text="🔧 Definições"
            android:textColor="#FFFFFF"
            android:textSize="11sp"
            android:nextFocusUp="@id/cardFilmes"
            android:nextFocusDown="@id/rvContinueWatching"
            android:nextFocusLeft="@id/btnQuickEpg"
            android:nextFocusRight="@id/btnQuickRadios" />

        <Button
            android:id="@+id/btnQuickRadios"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:layout_marginEnd="6dp"
            android:background="@drawable/bg_smarters_sage"
            android:text="📱 Emparelhar"
            android:textColor="#FFFFFF"
            android:textSize="11sp"
            android:nextFocusUp="@id/cardSeries"
            android:nextFocusDown="@id/rvContinueWatching"
            android:nextFocusLeft="@id/btnQuickSettings"
            android:nextFocusRight="@id/btnQuickCatchup" />

        <Button
            android:id="@+id/btnQuickCatchup"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:background="#2E7D32"
            android:text="🟢 Modo Fácil"
            android:textColor="#FFFFFF"
            android:textSize="11sp"
            android:textStyle="bold"
            android:nextFocusUp="@id/cardSeries"
            android:nextFocusDown="@id/rvContinueWatching"
            android:nextFocusLeft="@id/btnQuickRadios" />
    </LinearLayout>
'''

xml = re.sub(r'<LinearLayout[^>]*android:id="@+id/llQuickAccessBar".*?</LinearLayout>', new_bar, xml, flags=re.DOTALL)

with open('app/src/main/res/layout/activity_main.xml', 'w', encoding='utf-8') as f:
    f.write(xml)

print("Fixed layout")
