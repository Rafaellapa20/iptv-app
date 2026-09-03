# -*- coding: utf-8 -*-
with open('app/src/main/res/layout/activity_main.xml', 'r', encoding='utf-8') as f:
    text = f.read()

# Remove the MultiScreen button block
import re
text = re.sub(r'<Button\s+android:id=\"@\+id/btnQuickMultiScreen\".*?/>', '', text, flags=re.DOTALL)

# Fix nextFocus references
text = text.replace('android:nextFocusDown=\"@+id/btnQuickMultiScreen\"', 'android:nextFocusDown=\"@+id/btnQuickCatchup\"')
text = text.replace('android:nextFocusRight=\"@id/btnQuickMultiScreen\"', 'android:nextFocusRight=\"@id/btnQuickCatchup\"')
text = text.replace('android:nextFocusLeft=\"@id/btnQuickMultiScreen\"', 'android:nextFocusLeft=\"@id/btnQuickSettings\"')

with open('app/src/main/res/layout/activity_main.xml', 'w', encoding='utf-8') as f:
    f.write(text)
print("Done")
