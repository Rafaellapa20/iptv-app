# -*- coding: utf-8 -*-
import re

with open('app/src/main/res/layout/activity_main.xml', 'r', encoding='utf-8') as f:
    xml = f.read()

# Replace btnQuickSettings text
xml = re.sub(r'(android:id="@+id/btnQuickSettings"[^>]*?android:text=").*?(")', r'\g<1>⚙️ Definições\g<2>', xml, flags=re.DOTALL)
# Replace btnQuickRadios text
xml = re.sub(r'(android:id="@+id/btnQuickRadios"[^>]*?android:text=").*?(")', r'\g<1>📱 Emparelhar\g<2>', xml, flags=re.DOTALL)
# Replace btnQuickCatchup text and background
xml = re.sub(r'(android:id="@+id/btnQuickCatchup"[^>]*?android:text=").*?(")', r'\g<1>👴 Modo Fácil\g<2>', xml, flags=re.DOTALL)
xml = re.sub(r'(android:id="@+id/btnQuickCatchup"[^>]*?android:background=")@drawable/bg_smarters_sage(")', r'\g<1>#2E7D32\g<2>', xml, flags=re.DOTALL)

with open('app/src/main/res/layout/activity_main.xml', 'w', encoding='utf-8') as f:
    f.write(xml)

print("Forced buttons XML")
