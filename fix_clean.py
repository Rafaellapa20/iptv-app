with open('app/src/main/res/layout/activity_main.xml', 'r', encoding='utf-8') as f:
    text = f.read()

import xml.etree.ElementTree as ET

# Wait, ElementTree will mess up formatting and namespaces.
# Let's do it with precise indices.

# 1. Find btnQuickSettings and its nextFocusRight
idx_settings = text.find('android:id="@+id/btnQuickSettings"')
idx_next_right = text.find('android:nextFocusRight=', idx_settings)
# Replace the first occurrence of @id/btnQuickCatchup after btnQuickSettings
text = text[:idx_next_right] + text[idx_next_right:idx_next_right+100].replace('@id/btnQuickCatchup', '@id/btnQuickRadios') + text[idx_next_right+100:]

# 2. Find btnQuickRadios and its nextFocusRight
idx_radios = text.find('android:id="@+id/btnQuickRadios"')
idx_radios_right = text.find('android:nextFocusRight=', idx_radios)
# Replace whatever it is to @id/btnQuickCatchup
text = text[:idx_radios_right] + text[idx_radios_right:idx_radios_right+100].replace('@id/btnQuickRadios', '@id/btnQuickCatchup').replace('@id/btnQuickSettings', '@id/btnQuickCatchup') + text[idx_radios_right+100:]

# 3. Find btnQuickCatchup and its nextFocusLeft
idx_catchup = text.find('android:id="@+id/btnQuickCatchup"')
# Wait, dummy btnQuickCatchup might exist at top!
idx_catchup = text.rfind('android:id="@+id/btnQuickCatchup"')
idx_catchup_left = text.find('android:nextFocusLeft=', idx_catchup)
text = text[:idx_catchup_left] + text[idx_catchup_left:idx_catchup_left+100].replace('@id/btnQuickSettings', '@id/btnQuickRadios') + text[idx_catchup_left+100:]


with open('app/src/main/res/layout/activity_main.xml', 'w', encoding='utf-8') as f:
    f.write(text)

print("Fixed clean!")
