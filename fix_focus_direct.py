with open('app/src/main/res/layout/activity_main.xml', 'r', encoding='utf-8') as f:
    text = f.read()

# Just fix all of them directly
import re
text = re.sub(r'(android:id="@+id/btnQuickSettings".*?android:nextFocusRight=")@id/btnQuickCatchup"', r'\1@id/btnQuickRadios"', text, flags=re.DOTALL)
text = re.sub(r'(android:id="@+id/btnQuickCatchup".*?android:nextFocusLeft=")@id/btnQuickSettings"', r'\1@id/btnQuickRadios"', text, flags=re.DOTALL)

with open('app/src/main/res/layout/activity_main.xml', 'w', encoding='utf-8') as f:
    f.write(text)

print("Fixed focus direct!")
