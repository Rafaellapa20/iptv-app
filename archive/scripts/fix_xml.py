import re

with open('app/src/main/res/layout/activity_senior_main.xml', 'r', encoding='utf-8') as f:
    xml = f.read()

# Let's cleanly replace the entire shortcuts block
start_tag = '<!-- Category Row -->'
end_tag = '<!-- Footer -->'

block = xml[xml.find(start_tag):xml.find(end_tag)]
new_block = '''<!-- Category Row -->
        <TextView
            android:id="@+id/tvShortcutsTitle"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_below="@id/btnSeniorTvBig"
            android:layout_marginTop="24dp"
            android:text="Canais Recentes"
            android:textColor="#FFFFFF"
            android:textSize="20sp"
            android:textStyle="bold" />

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/rvSeniorRecentChannels"
            android:layout_width="match_parent"
            android:layout_height="120dp"
            android:layout_below="@id/tvShortcutsTitle"
            android:layout_marginTop="16dp" />

        '''

xml = xml[:xml.find(start_tag)] + new_block + xml[xml.find(end_tag):]

with open('app/src/main/res/layout/activity_senior_main.xml', 'w', encoding='utf-8') as f:
    f.write(xml)

print("Fixed XML")
