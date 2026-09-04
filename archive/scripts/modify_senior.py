import re

# 1. Update Layout
with open('app/src/main/res/layout/activity_senior_main.xml', 'r', encoding='utf-8') as f:
    xml = f.read()

# Replace the shortcuts LinearLayout with a RecyclerView
old_linear = r'<LinearLayout\s+android:layout_width="match_parent"\s+android:layout_height="120dp"\s+android:layout_below="@id/tvShortcutsTitle".*?</LinearLayout>'
new_recycler = '''<androidx.recyclerview.widget.RecyclerView
            android:id="@+id/rvSeniorRecentChannels"
            android:layout_width="match_parent"
            android:layout_height="120dp"
            android:layout_below="@id/tvShortcutsTitle"
            android:layout_marginTop="16dp" />'''

xml = re.sub(old_linear, new_recycler, xml, flags=re.DOTALL)

# Change title text
xml = xml.replace('Atalhos rápidos', 'Canais Recentes')
# Fix UTF-8 for tvShortcutsTitle if needed, it has 'Atalhos rÃ¡pidos' so we replace whatever text is there.
xml = re.sub(r'android:text="[^"]*rÃ¡pidos"', 'android:text="Canais Recentes"', xml)

with open('app/src/main/res/layout/activity_senior_main.xml', 'w', encoding='utf-8') as f:
    f.write(xml)

print("Layout updated.")
